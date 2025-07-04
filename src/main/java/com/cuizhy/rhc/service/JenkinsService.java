package com.cuizhy.rhc.service;

import com.cuizhy.rhc.constants.Constants;
import com.cuizhy.rhc.dao.ConfigDao;
import com.cuizhy.rhc.global.GlobalRequestManager;
import com.cuizhy.rhc.model.Info;
import com.cuizhy.rhc.util.FileUtil;
import com.cuizhy.rhc.util.JenkinsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.io.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class JenkinsService {

    @Autowired
    @Lazy
    private GlobalRequestManager requestManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfigDao configDao;

    @Autowired
    private JenkinsUtil jenkinsUtil;

    /**
     * 获取构建任务状态
     * @param jobName
     * @return
     * @throws Exception
     */
    public boolean getBuildStatus(String jobName) throws Exception {
        log.info("正在获取构建任务状态...");
        String jenkinsUrl = configDao.getValue("url", "jenkins");
        String job_info_url = configDao.getValue("job_info_url", "jenkins");
        String build_status_url = configDao.getValue("build_status_url", "jenkins");
        return jenkinsUtil.getBuildStatus(jenkinsUrl, job_info_url, build_status_url, jobName);
    }
}
