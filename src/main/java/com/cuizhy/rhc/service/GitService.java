package com.cuizhy.rhc.service;

import com.cuizhy.rhc.constants.Constants;
import com.cuizhy.rhc.dao.ConfigDao;
import com.cuizhy.rhc.model.Info;
import com.cuizhy.rhc.cache.CacheUtil;
import com.cuizhy.rhc.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class GitService {

    @Autowired
    private ConfigDao configDao;

    @Autowired
    private CacheUtil cacheUtil;

    @Autowired
    private JenkinsService jenkinsService;

    /**
     * 获取用户名
     * @return 用户名
     */
    private String getUserName(){
        return configDao.getValue("username","git");
    }

    /**
     * 获取 generate token
     * @see <a href="https://gitlab.crc.com.cn/-/user_settings/personal_access_tokens">创建令牌地址</a>
     * @return token
     */
    private String getGenerateToken(){
        return configDao.getValue("token","git");
    }


    public void gitClone(Info info,String cloneDir){
        Git git = null;
        try{
            info.setStatus(Constants.JOB_PROGRESS_GIT_CLONE,Constants.JOB_STATUS_RUNNING);
            cacheUtil.addInfoToJobList(info);
            File dir = new File(cloneDir);
            if (dir.exists()){
                log.info("删除目录：{}",dir.getAbsolutePath());
                FileUtil.deleteDir(cloneDir);
            }
            dir.mkdirs();

            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(info.getRepoUrl())
                    .setDirectory(new File(cloneDir))
                    .setBranch(info.getBranch())
                    .setDepth(1)
                    .setCloneAllBranches(false)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(this.getUserName(), this.getGenerateToken()))
                    .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)));
            git = cloneCommand.call();

            StoredConfig config = git.getRepository().getConfig();
            config.setInt("pack", null,"window", 250);
            // 配置 Delta 压缩深度（默认 50）
            config.setInt("pack", null, "depth", 100); // 增大深度以处理大文件
            // 配置压缩级别（0-9，9 为最高压缩）
            config.setInt("core", null, "compression", 9);
            config.setInt("core", null, "zstdCompressionLevel", 6);
            config.setLong("http", null, "postBuffer", 1024 * 1024 * 1024);
            config.save();

            info.setStatus(Constants.JOB_PROGRESS_GIT_CLONE,Constants.JOB_STATUS_SUCCESS);
            cacheUtil.addInfoToJobList(info);
            log.info("已克隆到 {}", git.getRepository().getDirectory().getParent());
        }catch (Exception e){
            info.setStatus(Constants.JOB_PROGRESS_GIT_CLONE,Constants.JOB_STATUS_FAIL);
            cacheUtil.addInfoToJobList(info);
            throw new RuntimeException("git clone 失败",e);
        }finally {
            if (git != null) {
                git.getRepository().close(); // 确保关闭Git对象
            }
        }
    }

    public Git gitCommit(String cloneDir) throws IOException, GitAPIException {
        log.info("正在提交...");
        Git git = Git.open(new File(cloneDir));

        //文件缓冲区
        git.getRepository().getConfig().setInt("http", null, "postBuffer", 524288000);
        //最大压缩
        git.getRepository().getConfig().setInt("core", null, "compression", 9);

        //git.add().addFilepattern(".").call();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = now.format(formatter);
        String commitMessage = String.format("rhc-builder commit and push at %s", timestamp);
        Status status = git.status().call();
        addOnlyModifiedFiles(git,status);
        if (hasChanges(status)){
            git.commit().setMessage(commitMessage).call();
            log.info("提交成功...");
            return git;
        }
        log.info("无变更,无需提交");
        return git;
    }

    public void addOnlyModifiedFiles(Git git,Status status) throws GitAPIException {
        // 添加所有修改（Modified）和未跟踪（Untracked）的文件
        // 2. 批量添加所有变更文件（兼容全版本）
        if (!status.getModified().isEmpty() || !status.getUntracked().isEmpty()) {
            AddCommand add = git.add();
            status.getModified().forEach(add::addFilepattern);
            status.getUntracked().forEach(add::addFilepattern);
            add.call();
        }
        // 2. 删除文件（兼容写法）
        if (!status.getRemoved().isEmpty()) {
            RmCommand rm = git.rm();
            status.getRemoved().forEach(rm::addFilepattern);
            rm.call(); // 单次提交所有删除
        }
    }

    public Git gitGc(Git git) throws GitAPIException, IOException {
        git.getRepository().close(); // 关闭现有实例
        File gitDir = git.getRepository().getDirectory();

        // 重新打开新的 Git 实例
        Git freshGit = Git.open(gitDir);

        // 执行 GC
        freshGit.gc()
                .setAggressive(true)
                .setPreserveOldPacks(false)
                .call();

        return freshGit; // 返回新实例
    }

    public void gitPush(Git git) throws GitAPIException {
        log.info("正在推送...");
        Status status = git.status().call();
        if (hasChanges(status)){
            git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(this.getUserName(), this.getGenerateToken()))
                    .setThin(true)
                    .setAtomic(true)
                    .call();
            log.info("推送成功...");
            return;
        }
        log.info("无变更,无需推送");
    }

    private boolean hasChanges(Status status) {
        return !status.getModified().isEmpty() ||
                !status.getUntracked().isEmpty() ||
                !status.getRemoved().isEmpty();
    }

    public void gitCommitAndPush(Info info){
        Git git = null;
        try{
            info.setStatus(Constants.JOB_PROGRESS_GIT_COMMIT_AND_PUSH,Constants.JOB_STATUS_RUNNING);
            cacheUtil.addInfoToJobList(info);
            git = this.gitCommit(this.getCloneDir(info));
            this.gitGc(git);
            this.gitPush(git);
            info.setStatus(Constants.JOB_PROGRESS_GIT_COMMIT_AND_PUSH,Constants.JOB_STATUS_SUCCESS);
            cacheUtil.addInfoToJobList(info);
        }catch (Exception e){
            info.setStatus(Constants.JOB_PROGRESS_GIT_COMMIT_AND_PUSH,Constants.JOB_STATUS_FAIL);
            cacheUtil.addInfoToJobList(info);
            throw new RuntimeException("git commit and push 失败",e);
        }finally {
            if (git != null) {
                git.getRepository().close();
            }
        }
    }

    public void gitPull() {
        try {
            Git git = Git.open(new File(this.getCloneDir((Info) cacheUtil.get("info"))));
            git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(this.getUserName(), this.getGenerateToken())).call();
        } catch (IOException | GitAPIException e) {
            log.error("git pull 失败",e);
        }
    }

    /**
     * 获取克隆目录
     * @param info 构建目标
     * @return 克隆目录
     */
    public String getCloneDir(Info info) {
        return FileUtil.getRuntimeAbsolutePath()+File.separator + Constants.GIT_CLONE_DIR + File.separator+ info.getName();
    }

    public void copyFile(Info info){
        try{
            info.setStatus(Constants.JOB_PROGRESS_COPY_FILE,Constants.JOB_STATUS_RUNNING);
            cacheUtil.addInfoToJobList(info);
            String cloneDir = this.getCloneDir(info);
            String downloadFilePath = jenkinsService.getDownloadPath(info);

            Path sourceFile = Paths.get(downloadFilePath);
            Path targetDir = Paths.get(cloneDir);
            Path targetFile = targetDir.resolve(sourceFile.getFileName());

            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            if (info.getIsFront() == 1){
                //需要解压dist.zip ,删除cloneDir里边的文件夹，拷贝dist.zip文件夹到cloneDir 然后删除dist.zip
                String zipFilePath = Paths.get(cloneDir, "dist.zip").toString();

                // 删除目标目录中的指定文件夹
                FileUtil.deleteDir(Paths.get(cloneDir, Constants.FRONT_DIR_UAT).toString());
                FileUtil.deleteDir(Paths.get(cloneDir, Constants.FRONT_DIR_PROD).toString());

                // 解压 dist.zip 文件
                if (zipFilePath.endsWith(".zip")) {
                    try {
                        FileUtil.unzipFile(zipFilePath, cloneDir);
                        log.info("已解压文件: {} 到 {}", zipFilePath, cloneDir);

                        // 重命名解压后的文件夹
                        String dirName = "dist";
                        if (info.getEnv().equals("prod")){
                            dirName = Constants.FRONT_DIR_PROD;
                        }
                        if (info.getEnv().equals("uat")){
                            dirName = Constants.FRONT_DIR_UAT;
                        }
                        Path extractedDir = Paths.get(cloneDir, "dist");
                        if (Files.exists(extractedDir)) {
                            Path newDirPath = Paths.get(cloneDir, dirName);
                            Files.move(extractedDir, newDirPath, StandardCopyOption.REPLACE_EXISTING);
                            log.info("已将解压的文件夹重命名为: {}", newDirPath);
                        } else {
                            log.info("解压后找不到 dist 文件夹");
                        }

                        // 删除 dist.zip 文件
                        Files.deleteIfExists(Paths.get(zipFilePath));
                        log.info("已删除源文件: {}", zipFilePath);

                    } catch (IOException e) {
                        throw new RuntimeException("处理文件失败: " + zipFilePath, e);
                    }
                }
            }
            info.setStatus(Constants.JOB_PROGRESS_COPY_FILE,Constants.JOB_STATUS_SUCCESS);
            cacheUtil.addInfoToJobList(info);
            log.info("文件成功拷贝到: {}", targetFile);
        }catch (Exception e){
            info.setStatus(Constants.JOB_PROGRESS_COPY_FILE,Constants.JOB_STATUS_FAIL);
            cacheUtil.addInfoToJobList(info);
            throw new RuntimeException("文件拷贝失败",e);
        }
    }

}
