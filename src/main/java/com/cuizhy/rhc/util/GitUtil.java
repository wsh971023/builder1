package com.cuizhy.rhc.util;

import com.cuizhy.rhc.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

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
    public static void forceCleanAndPush(String remote_repo_url,String username,String password) throws IOException, GitAPIException {
        String local_repo_dir = getCloneDir(remote_repo_url);
        try (Git git = Git.open(new File(local_repo_dir))) {
            Repository repository = git.getRepository();
            //自动检测当前分支名称
            String targetBranchName = repository.getBranch();

            Iterable<RevCommit> logInfo = git.log().setMaxCount(1).call();
            RevCommit latestCommit = logInfo.iterator().next();
            //获取最近一次提交信息
            String originalCommitMessage = latestCommit.getFullMessage();

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
        }
    }
}
