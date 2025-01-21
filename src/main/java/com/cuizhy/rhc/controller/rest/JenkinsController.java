package com.cuizhy.rhc.controller.rest;

import com.cuizhy.rhc.constants.Constants;
import com.cuizhy.rhc.dao.ConfigDao;
import com.cuizhy.rhc.dao.InfoDao;
import com.cuizhy.rhc.model.Info;
import com.cuizhy.rhc.cache.CacheUtil;
import com.cuizhy.rhc.model.Status;
import com.cuizhy.rhc.service.GitService;
import com.cuizhy.rhc.service.JenkinsService;
import com.cuizhy.rhc.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/jenkins")
public class JenkinsController {

    @Autowired
    private JenkinsService jenkinsService;
    @Autowired
    private GitService gitService;

    @Autowired
    private ConfigDao configDao;
    @Autowired
    private InfoDao infoDao;
    @Autowired
    private CacheUtil cacheUtil;

    @RequestMapping("/single-start")
    public void singleStart(@RequestBody Map<String,String> data) throws Exception {
        String username = configDao.getValue("username", "jenkins");
        String password = configDao.getValue("password", "jenkins");
        String url = configDao.getValue("url", "jenkins");
        String crumb_url = configDao.getValue("crumb_url", "jenkins");
        String job_info_url = configDao.getValue("job_info_url", "jenkins");
        String build_status_url = configDao.getValue("build_status_url", "jenkins");
        Thread.ofVirtual().start(() -> {
            String work = data.get("work");
            String env = data.get("env");
            String build = data.get("build");
            Info info = infoDao.getInfo(work, env);
            jenkinsService.loginToJenkins(url, username, password);
            jenkinsService.getJenkinsCrumb(url + crumb_url);
            cacheUtil.set(Constants.JENKINS_LOGIN_CACHE_KEY,Constants.JOB_STATUS_SUCCESS);
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_LOGIN,Constants.JOB_STATUS_SUCCESS);
            cacheUtil.addInfoToJobList(info);
            boolean need_build = "on".equalsIgnoreCase(build);
            if (need_build){
                jenkinsService.triggerBuild(url, info);
            }else{
                info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_SUCCESS);
            }
            String download_path = info.getFilepath();
            // 获取 JAR 包所在的路径
            String jarDirPath = FileUtil.getRuntimeAbsolutePath() + File.separator + Constants.JENKINS_DOWNLOAD_DIR;
            Integer number = null;
            try {
                number = jenkinsService.getBuildNumber(url,job_info_url,info.getJobName(),need_build);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                jenkinsService.checkBuildStatus(url,build_status_url, number, info);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Thread.ofVirtual().start(()->{
                jenkinsService.downloadFile(url,download_path,jarDirPath,info);
            });
            Thread.ofVirtual().start(()->{
                gitService.gitClone(info, gitService.getCloneDir(info));
            });
            while (
                    info.getStatus(Constants.JOB_PROGRESS_JENKINS_DOWNLOAD).getStatus().equals(Constants.JOB_STATUS_SUCCESS)
                    && info.getStatus(Constants.JOB_PROGRESS_GIT_CLONE).getProcess().equals(Constants.JOB_STATUS_SUCCESS)
            ){
                gitService.copyFile(info);
                gitService.gitCommitAndPush(info);
            }
        });
    }

    @RequestMapping("/login")
    public boolean login() throws Exception {
        String username = configDao.getValue("username", "jenkins");
        String password = configDao.getValue("password", "jenkins");
        String url = configDao.getValue("url", "jenkins");
        String crumb_url = configDao.getValue("crumb_url", "jenkins");

        Thread.ofVirtual().start(() -> {
            jenkinsService.loginToJenkins(url, username, password);
            jenkinsService.getJenkinsCrumb(url + crumb_url);
            cacheUtil.set(Constants.JENKINS_LOGIN_CACHE_KEY,Constants.JOB_STATUS_SUCCESS);
        });
        return true;
    }

    @RequestMapping("/build")
    public void build(@RequestBody Map<String,String> data) {
        String build = data.get("build");
        String work = data.get("work");
        String env = data.get("env");

        Info cacheInfo = cacheUtil.getInfoFromJobList(env,work);
        if (cacheInfo!=null){
            return;
        }
        Info info = infoDao.getInfo(work, env);
        if (info !=null){
            cacheUtil.addInfoToJobList(info);
        }
        Status status = info.getStatus(Constants.JOB_PROGRESS_JENKINS_BUILD);
        if (Constants.JOB_STATUS_RUNNING.equalsIgnoreCase(status.getStatus())){
            return;
        }

        if (Constants.JOB_STATUS_INIT.equals(status.getStatus()) && !"on".equalsIgnoreCase(build)){
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_SUCCESS);
            return;
        }

        info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_RUNNING);

        String url = configDao.getValue("url", "jenkins");
        Thread.ofVirtual().start(() -> {
            jenkinsService.triggerBuild(url, info);
        });
    }

    @RequestMapping("/get-job-status")
    public boolean getJobStatus() throws Exception {
        String job_name = (String) cacheUtil.get("job");
        return jenkinsService.getBuildStatus(job_name);
    }

    @RequestMapping("/download-file")
    public void downloadFile() throws Exception {
        String url = configDao.getValue("url", "jenkins");
        Info info = (Info) cacheUtil.get("info");
        String job = (String) cacheUtil.get("job");
        String download_path = info.getFilepath();
        // 获取 JAR 包所在的路径
        String jarDirPath = FileUtil.getRuntimeAbsolutePath() + File.separator + Constants.JENKINS_DOWNLOAD_DIR;

        jenkinsService.downloadFile(url,download_path,jarDirPath,info);
    }
}
