package com.cuizhy.rhc.controller.rest;

import com.cuizhy.rhc.cache.CacheUtil;
import com.cuizhy.rhc.dao.InfoDao;
import com.cuizhy.rhc.model.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/status")
public class JobStatusController {

    @Autowired
    private CacheUtil cacheUtil;
    @Autowired
    private InfoDao infoDao;

    @RequestMapping("/get")
    public Info getStatus(@RequestBody Map<String,String> data) {
        String env = data.get("env");
        String work = data.get("work");
        Info info = infoDao.getInfo(work, env);
        return cacheUtil.getInfoFromJobList(env,info.getJobName());
    }
}
