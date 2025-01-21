package com.cuizhy.rhc.controller.rest;

import com.cuizhy.rhc.model.Info;
import com.cuizhy.rhc.cache.CacheUtil;
import com.cuizhy.rhc.service.GitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/git")
public class GitController {

    @Autowired
    private CacheUtil cacheUtil;

    @Autowired
    private GitService gitService;

    @RequestMapping("/git-clone")
    public void gitClone(){
        // 克隆仓库 URL
        Info info = (Info) cacheUtil.get("info");
        // 克隆目录
        String cloneDir = gitService.getCloneDir(info);

        Thread thread = new Thread(() -> {
            try {
                gitService.gitClone(info,cloneDir);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        thread.start(); // 启动线程
    }

    @RequestMapping("/copy-file")
    public void copyFile() throws Exception {
        Info info = (Info) cacheUtil.get("info");
        gitService.copyFile(info);
    }

    @RequestMapping("/git-commit-and-push")
    public void gitCommitAndPush() {
        Info info = (Info) cacheUtil.get("info");
        Thread thread = new Thread(() -> {
            try {
                gitService.gitCommitAndPush(info);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        thread.start(); // 启动线程;
    }
}
