package com.cuizhy.rhc.service;

import com.cuizhy.rhc.cache.CacheUtil;
import com.cuizhy.rhc.dao.InfoDao;
import com.cuizhy.rhc.model.Info;
import com.cuizhy.rhc.vo.TaskSubmitPVO;
import com.cuizhy.rhc.vo.TaskSubmitRVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowService {

    @Autowired
    private InfoDao infoDao;

    @Autowired
    private CacheUtil cacheUtil;

    @Autowired
    private WorkService workService;

    public TaskSubmitRVO start(TaskSubmitPVO taskSubmitPVO) throws Exception {
        String work = taskSubmitPVO.getWork();
        String env = taskSubmitPVO.getEnv();
        Info info = infoDao.getInfo(work, env);
        taskSubmitPVO.setInfoConfig(info);
        cacheUtil.addInfoToJobList(info);
        workService.jenkinsLogin(taskSubmitPVO);
        boolean isBuilding = workService.jenkinsCheckBuilding(taskSubmitPVO);
        TaskSubmitRVO taskSubmitRVO = new TaskSubmitRVO(isBuilding);
        Thread.ofPlatform().start(()->{
            try {
                workService.jenkinsBuild(taskSubmitPVO);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Thread.ofPlatform().start(()->{
                try {
                    workService.waitJenkinsBuilding(taskSubmitPVO);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                workService.downloadJenkinsFile(taskSubmitPVO);
            });
            Thread.ofPlatform().start(()->{
                workService.gitClone(taskSubmitPVO);
            });
            workService.waitCopyFile(taskSubmitPVO);//此处阻塞等待
            workService.copyFile(taskSubmitPVO);
            workService.gitCommitAndPush(taskSubmitPVO);
        });
        return taskSubmitRVO;
    }
}
