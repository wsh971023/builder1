package com.cuizhy.rhc.util;

import com.cuizhy.rhc.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class GitUtil {

    /**
     * 禁止外部创建对象
     */
    private GitUtil() {}

    /**
     * git clone
     * @param remote_repo_url git仓库地址
     * @param branch_name 分支名称
     * @param username 用户名
     * @param password 密码
     */
    public static void gitClone(String remote_repo_url, String branch_name, String username,String password) throws GitAPIException, IOException {
        String cloneDir = getCloneDir(remote_repo_url);
        File dir = new File(cloneDir);

        if (dir.exists()) {
            log.warn("目录 {} 已存在，将根据业务要求进行删除并重新克隆。", dir.getAbsolutePath());
            FileUtil.deleteDir(cloneDir);
        }

        try (Git result = Git.cloneRepository()
                .setURI(remote_repo_url)
                .setDirectory(dir)
                .setBranch(branch_name)
                .setDepth(1)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
                .call()) {
            log.info("仓库已成功克隆到 {}", result.getRepository().getDirectory().getParent());
        }
    }

    /**
     * git commit
     * @param remote_repo_url git仓库地址
     * @param commit_message 提交信息
     * @throws IOException IO 异常
     * @throws GitAPIException Git API 异常
     */
    public static void gitCommit(String remote_repo_url, String commit_message) throws IOException, GitAPIException {
        String local_repo_dir = getCloneDir(remote_repo_url);
        try (Git git = Git.open(new File(local_repo_dir))) {
            //文件缓冲区
            git.getRepository().getConfig().setInt("http", null, "postBuffer", 524288000);
            //最大压缩
            git.getRepository().getConfig().setInt("core", null, "compression", 9);
            git.add().addFilepattern(".").call();
            git.commit().setAll(true).setMessage(commit_message).call();
        }
    }

    /**
     * git提交
     * @param remote_repo_url git仓库地址
     * @param username 用户名
     * @param password 密码
     * @throws GitAPIException git异常
     * @throws IOException io异常
     */
    public static void gitPush(String remote_repo_url,String username,String password) throws GitAPIException, IOException {
        String local_repo_dir = getCloneDir(remote_repo_url);
        try (Git git = Git.open(new File(local_repo_dir))) {
            git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call();
        }
    }

    /**
     * 获取克隆的仓库目录
     * @param remote_repo_url 远程仓库 URL
     * @return 克隆的本地仓库目标目录
     */
    public static String getCloneDir(String remote_repo_url) {
        if (remote_repo_url == null || remote_repo_url.isBlank()) {
            throw new IllegalArgumentException("远程仓库URL不能为空");
        }
        int lastSlashIndex = remote_repo_url.lastIndexOf('/');
        if (lastSlashIndex == -1 || lastSlashIndex >= remote_repo_url.length() - 1) {
            throw new IllegalArgumentException("无效的远程仓库URL格式: " + remote_repo_url);
        }

        String repoName = remote_repo_url.substring(lastSlashIndex + 1);
        // 更安全地移除.git后缀
        if (repoName.endsWith(".git")) {
            repoName = repoName.substring(0, repoName.length() - 4);
        }

        // 使用Paths.get来跨平台地、安全地构建文件路径
        return Paths.get(FileUtil.getRuntimeAbsolutePath(), Constants.GIT_CLONE_DIR, repoName).toString();
    }

    /**
     * 使用孤儿分支强制清理及推送当前分支进行覆盖
     * @param remote_repo_url
     * @param username
     * @param password
     * @throws IOException
     * @throws GitAPIException
     */
    public static void forceCleanAndPush(String remote_repo_url,String username,String password,String project_id) throws IOException, GitAPIException {
        String local_repo_dir = getCloneDir(remote_repo_url);
        try (Git git = Git.open(new File(local_repo_dir))) {
            Repository repository = git.getRepository();
            //自动检测当前分支名称
            String targetBranchName = repository.getBranch();

            Iterable<RevCommit> logInfo = git.log().setMaxCount(2).call(); // 获取最近两次提交
            List<RevCommit> commits = new ArrayList<>();
            for (RevCommit commit : logInfo) {
                commits.add(commit);
            }

            RevCommit latestCommit = commits.get(0);   // 最新提交
            RevCommit previousCommit = commits.size() > 1 ? commits.get(1) : null; // 上一次提交（可能不存在）

            //获取最近一次提交信息
            String originalCommitMessage = latestCommit.getFullMessage();
            assert previousCommit != null;
            int commitTimeSeconds = previousCommit.getCommitTime(); // Unix 时间戳（秒）
            LocalDate commitDate = Instant.ofEpochSecond(commitTimeSeconds)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            String tempBranchName = "new-history-" + System.currentTimeMillis();
            log.info("--- 步骤 2/5: 正在创建新的孤儿分支 '{}'... ---", tempBranchName);
            git.checkout().setOrphan(true).setName(tempBranchName).call();

            // 步骤 3: 将 depth 1 的内容提交到新分支
            log.info("--- 步骤 3/5: 正在将最新文件作为初始提交... ---");
            git.add().addFilepattern(".").call();
            git.commit().setMessage(originalCommitMessage).call();
            log.info("新历史的初始提交已创建。");

            // 步骤 4 & 5: 将新历史强制应用到目标分支，并清理临时分支
            log.info("--- 步骤 4/5: 正在将新历史强制更新到 '{}'... ---", targetBranchName);
            // 使用 branchCreate() 和 setForce(true) 来模拟 'git branch -M'。
            // 这会强制将 targetBranchName 分支的指针移动到我们当前所在的新历史的 HEAD
            git.branchCreate()
                    .setName(targetBranchName)
                    .setForce(true)
                    .call();

            // 切换到更新后的目标分支
            git.checkout().setName(targetBranchName).call();

            // 删除不再需要的临时分支
            git.branchDelete().setBranchNames(tempBranchName).call();
            log.info("分支 '{}' 已成功指向新历史。", targetBranchName);

            // 步骤 6: 强制推送
            log.info("--- 步骤 5/5: 正在强制推送到远程仓库... ---");
            git.push().setForce(true).add(targetBranchName).setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call();
            log.info("--- 推送成功 ---");

            // 判断是否是今天
            LocalDate today = LocalDate.now();
            if (!commitDate.equals(today)) {
                log.info("最新提交时间不是今天，本次触发 Housekeeping");
                gitlabHouseKeeping(remote_repo_url,project_id,password);
            }else{
                log.info("最新提交时间是今天，本次不触发 Housekeeping");
            }
        }
    }

    /**
     * 触发 GitLab 仓库 Housekeeping
     * @param repoUrl      仓库 URL，例如 https://gitlab.example.com/aaa/b.git
     * @param projectId    项目 ID 或 URL 编码后的路径，例如 aaa%2Fb
     * @param privateToken GitLab Personal Access Token
     */
    public static void gitlabHouseKeeping(String repoUrl, String projectId, String privateToken) {
        if (repoUrl == null || projectId == null || privateToken == null) {
            log.error("仓库 URL、项目 ID 或 Token 不能为空");
            return;
        }

        try {
            // 每次调用都创建新的 HttpClient
            HttpClient client = HttpClient.newHttpClient();

            // 构造 API 地址
            String host = extractHostWithProtocol(repoUrl);
            if (host == null) {
                log.error("无法解析仓库 URL 的 host: {}", repoUrl);
                return;
            }
            String apiUrl = host + "/api/v4/projects/" + projectId + "/housekeeping";

            // 构造请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("PRIVATE-TOKEN", privateToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            // 发送请求
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 输出完整报文
            log.info("Housekeeping Response Code: {}, Headers: {}, Body: {}",
                    response.statusCode(),
                    response.headers().map(),
                    response.body());

        } catch (IOException | InterruptedException e) {
            log.error("触发仓库 Housekeeping 失败", e);
        }
    }

    /**
     * 提取 URL 的协议 + host 部分
     * 例如：https://gitlab.example.com/aaa/b.git -> https://gitlab.example.com
     * @param url 仓库 URL
     * @return 协议 + host，失败返回 null
     */
    private static String extractHostWithProtocol(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        Pattern pattern = Pattern.compile("^(https?://[^/]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
