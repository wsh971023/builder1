package com.cuizhy.rhc.service;

import com.cuizhy.rhc.annotation.ProcessPoint;
import com.cuizhy.rhc.cache.CacheUtil;
import com.cuizhy.rhc.constants.Constants;
import com.cuizhy.rhc.dao.ConfigDao;
import com.cuizhy.rhc.dao.InfoDao;
import com.cuizhy.rhc.model.Info;
import com.cuizhy.rhc.util.FileUtil;
import com.cuizhy.rhc.util.JenkinsUtil;
import com.cuizhy.rhc.vo.TaskSubmitPVO;
import com.cuizhy.rhc.vo.TaskSubmitRVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@Service
public class WorkService {

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

    @Autowired
    private JenkinsUtil jenkinsUtil;

    public TaskSubmitRVO start(TaskSubmitPVO taskSubmitPVO) throws Exception {
        String work = taskSubmitPVO.getWork();
        String env = taskSubmitPVO.getEnv();
        Info info = infoDao.getInfo(work, env);
        taskSubmitPVO.setInfoConfig(info);
        cacheUtil.addInfoToJobList(info);
        this.jenkinsLogin(taskSubmitPVO);
        boolean isBuilding = this.jenkinsCheckBuilding(taskSubmitPVO);
        TaskSubmitRVO taskSubmitRVO = new TaskSubmitRVO(isBuilding);
        Thread.ofPlatform().start(()->{
            try {
                this.jenkinsBuild(taskSubmitPVO);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Thread.ofPlatform().start(()->{
                try {
                    this.waitJenkinsBuilding(taskSubmitPVO);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                this.downloadJenkinsFile(taskSubmitPVO);
            });
            Thread.ofPlatform().start(()->{
                this.gitClone(taskSubmitPVO);
            });
            this.copyFile(taskSubmitPVO);
            this.gitCommitAndPush(taskSubmitPVO);
        });
        return taskSubmitRVO;
    }

    @ProcessPoint(progress = Constants.JOB_PROGRESS_JENKINS_LOGIN)
    public void jenkinsLogin(TaskSubmitPVO taskSubmitPVO){
        String username = configDao.getValue("username", "jenkins");
        String password = configDao.getValue("password", "jenkins");
        String url = configDao.getValue("url", "jenkins");
        String crumb_url = configDao.getValue("crumb_url", "jenkins");
        jenkinsUtil.loginToJenkins(url, username, password);
        jenkinsUtil.getJenkinsCrumb(url + crumb_url);
    }

    /**
     * 触发jenkins构建
     * @param taskSubmitPVO
     * @throws Exception
     */
    public void jenkinsBuild(TaskSubmitPVO taskSubmitPVO) throws Exception {
        String build = taskSubmitPVO.getBuild();
        String work = taskSubmitPVO.getWork();
        String env = taskSubmitPVO.getEnv();
        String url = configDao.getValue("url", "jenkins");
        Info info = cacheUtil.getInfoFromJobList(env,work);
        boolean need_build = "on".equalsIgnoreCase(build);
        boolean isBuilding = this.jenkinsCheckBuilding(taskSubmitPVO);
        if (need_build && !isBuilding){
            jenkinsUtil.triggerBuild(url, info.getJobName());
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_RUNNING);
        }else if (isBuilding){
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_RUNNING);
        }
        cacheUtil.addInfoToJobList(info);
    }

    
    public boolean jenkinsCheckBuilding(TaskSubmitPVO taskSubmitPVO) throws Exception {
        String work = taskSubmitPVO.getWork();
        String env = taskSubmitPVO.getEnv();
        Info info = cacheUtil.getInfoFromJobList(env,work);
        String url = configDao.getValue("url", "jenkins");
        String job_info_url = configDao.getValue("job_info_url", "jenkins");
        String build_status_url = configDao.getValue("build_status_url", "jenkins");
        log.info("check status before jenkins build... ");
        int number = jenkinsUtil.getBuildNumber(url,job_info_url,info.getJobName(),false);
        log.info("before build number: {}",number);
        return jenkinsUtil.getBuildStatus(url,job_info_url,build_status_url, info.getJobName());
    }

    
    public void waitJenkinsBuilding(TaskSubmitPVO taskSubmitPVO) throws Exception {
        String url = configDao.getValue("url", "jenkins");
        String build_status_url = configDao.getValue("build_status_url", "jenkins");
        String job_info_url = configDao.getValue("job_info_url", "jenkins");
        String work = taskSubmitPVO.getWork();
        String env = taskSubmitPVO.getEnv();
        Info info = cacheUtil.getInfoFromJobList(env,work);
        int number = jenkinsUtil.getBuildNumber(url,job_info_url,info.getJobName(),true);
        try {
            jenkinsUtil.checkBuildStatus(url,build_status_url, number, info);
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_SUCCESS);
            cacheUtil.addInfoToJobList(info);
        }catch (Exception e){
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_FAIL);
            cacheUtil.addInfoToJobList(info);
        }

    }

    
    public void downloadJenkinsFile(TaskSubmitPVO taskSubmitPVO){
        String url = configDao.getValue("url", "jenkins");
        String work = taskSubmitPVO.getWork();
        String env = taskSubmitPVO.getEnv();
        Info info = cacheUtil.getInfoFromJobList(env,work);
        String download_path = info.getFilepath();
        String jarDirPath = FileUtil.getRuntimeAbsolutePath() + File.separator + Constants.JENKINS_DOWNLOAD_DIR;
        info.setStatus(Constants.JOB_PROGRESS_JENKINS_DOWNLOAD,Constants.JOB_STATUS_RUNNING);
        cacheUtil.addInfoToJobList(info);
        try{
            jenkinsUtil.downloadFile(url,download_path,jarDirPath,info.getJobName());
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_DOWNLOAD,Constants.JOB_STATUS_SUCCESS);
            cacheUtil.addInfoToJobList(info);
        }catch (Exception e){
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_DOWNLOAD,Constants.JOB_STATUS_FAIL);
            cacheUtil.addInfoToJobList(info);
        }

    }

    public void gitClone(TaskSubmitPVO taskSubmitPVO){
        String work = taskSubmitPVO.getWork();
        String env = taskSubmitPVO.getEnv();
        Info info = cacheUtil.getInfoFromJobList(env,work);
        info.setStatus(Constants.JOB_PROGRESS_GIT_CLONE,Constants.JOB_STATUS_RUNNING);
        cacheUtil.addInfoToJobList(info);
        try{
            gitService.gitClone(info);
            info.setStatus(Constants.JOB_PROGRESS_GIT_CLONE,Constants.JOB_STATUS_SUCCESS);
            cacheUtil.addInfoToJobList(info);
        }catch (Exception e){
            info.setStatus(Constants.JOB_PROGRESS_GIT_CLONE,Constants.JOB_STATUS_FAIL);
            cacheUtil.addInfoToJobList(info);
        }
    }

    public void copyFile(TaskSubmitPVO taskSubmitPVO){
        String env = taskSubmitPVO.getEnv();
        String work = taskSubmitPVO.getWork();
        while (true){
            LockSupport.parkNanos(1000 * 1_000_000); // 暂停一秒
            log.debug("wait jenkins download or git clone...");
            Info infoCache = cacheUtil.getInfoFromJobList(env,work);
            if (infoCache.getStatus(Constants.JOB_PROGRESS_JENKINS_DOWNLOAD).getStatus().equals(Constants.JOB_STATUS_SUCCESS)
                    && infoCache.getStatus(Constants.JOB_PROGRESS_GIT_CLONE).getStatus().equals(Constants.JOB_STATUS_SUCCESS)){
                infoCache.setStatus(Constants.JOB_PROGRESS_GIT_COMMIT_AND_PUSH,Constants.JOB_STATUS_RUNNING);
                cacheUtil.addInfoToJobList(infoCache);
                try{
                    gitService.copyFile(infoCache);
                    infoCache.setStatus(Constants.JOB_PROGRESS_GIT_COMMIT_AND_PUSH,Constants.JOB_STATUS_SUCCESS);
                    cacheUtil.addInfoToJobList(infoCache);
                }catch (Exception e){
                    infoCache.setStatus(Constants.JOB_PROGRESS_GIT_COMMIT_AND_PUSH,Constants.JOB_STATUS_FAIL);
                    cacheUtil.addInfoToJobList(infoCache);
                }
                break;
            }
        }
    }


    public void gitCommitAndPush(TaskSubmitPVO taskSubmitPVO){
        String env = taskSubmitPVO.getEnv();
        String work = taskSubmitPVO.getWork();
        Info info = cacheUtil.getInfoFromJobList(env,work);
        info.setStatus(Constants.JOB_PROGRESS_GIT_COMMIT_AND_PUSH,Constants.JOB_STATUS_RUNNING);
        cacheUtil.addInfoToJobList(info);
        try{
            gitService.gitCommitAndPush(info);
            info.setStatus(Constants.JOB_PROGRESS_GIT_COMMIT_AND_PUSH,Constants.JOB_STATUS_SUCCESS);
            cacheUtil.addInfoToJobList(info);
        }catch (Exception e){
            info.setStatus(Constants.JOB_PROGRESS_GIT_COMMIT_AND_PUSH,Constants.JOB_STATUS_FAIL);
            cacheUtil.addInfoToJobList(info);
        }
    }
}
