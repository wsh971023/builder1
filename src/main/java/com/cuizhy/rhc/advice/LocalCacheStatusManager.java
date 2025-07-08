package com.cuizhy.rhc.advice;

import com.cuizhy.rhc.cache.CacheUtil;
import com.cuizhy.rhc.model.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("default") // 使用Profile方便未来切换
public class LocalCacheStatusManager implements IStatusManager {

    @Autowired
    private CacheUtil cacheUtil;

    @Override
    public void update(Info info, String progress, String status, Throwable error) {
        info.setStatus(progress, status, error);
        cacheUtil.addInfoToJobList(info);
        if (error != null) {
            log.error("任务进度 [{}] 失败", progress, error);
        }
    }

    @Override
    public Object getStatusInfo() {
        return cacheUtil.getDb0();
    }
}