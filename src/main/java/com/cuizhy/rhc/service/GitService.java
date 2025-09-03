package com.cuizhy.rhc.service;

import com.cuizhy.rhc.dao.ConfigDao;
import com.cuizhy.rhc.model.Info;
import com.cuizhy.rhc.util.FileUtil;
import com.cuizhy.rhc.util.GitUtil;
import com.cuizhy.rhc.util.JenkinsUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    private JenkinsUtil jenkinsUtil;

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


    @SneakyThrows
    public void gitClone(Info info){
        GitUtil.gitClone(info.getRepoUrl(),info.getBranch(),getUserName(),getGenerateToken());
    }

    @SneakyThrows
    private void gitCommit(String remote_repo_url){
        log.info("正在提交...");
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = now.format(formatter);
        String commitMessage = String.format("rhc-builder commit and push at %s", timestamp);
        GitUtil.gitCommit(remote_repo_url,commitMessage);
    }

    @SneakyThrows
    public void gitCommitAndPush(Info info){
        this.gitCommit(info.getRepoUrl());
        GitUtil.forceCleanAndPush(info.getRepoUrl(),getUserName(),getGenerateToken());
    }

    @SneakyThrows
    public void copyFile(Info info){
        String cloneDir = GitUtil.getCloneDir(info.getRepoUrl());
        String downloadFilePath = jenkinsUtil.getDownloadPath(info.getFilepath());

        Path sourceFile = Paths.get(downloadFilePath);
        Path targetDir = Paths.get(cloneDir);
        Path targetFile = targetDir.resolve(sourceFile.getFileName());

        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

        if (info.getIsFront() == 1) {
            //需要解压dist.zip ,删除cloneDir里边的文件夹，拷贝dist.zip文件夹到cloneDir 然后删除dist.zip
            String zipFilePath = Paths.get(cloneDir, "dist.zip").toString();

            // 删除目标目录中的指定文件夹
            FileUtil.deleteDir(Paths.get(cloneDir, info.getFrontDirName()).toString());

            // 解压 dist.zip 文件
            if (zipFilePath.endsWith(".zip")) {
                try {
                    FileUtil.unzipFile(zipFilePath, cloneDir);
                    log.info("已解压文件: {} 到 {}", zipFilePath, cloneDir);

                    // 重命名解压后的文件夹
                    String dirName = info.getFrontDirName();
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
        log.info("文件成功拷贝到: {}", targetFile);
    }

}
