package com.cuizhy.rhc.advice;

import com.cuizhy.rhc.annotation.ProcessPoint;
import com.cuizhy.rhc.cache.CacheUtil;
import com.cuizhy.rhc.constants.Constants;
import com.cuizhy.rhc.model.Info;
import com.cuizhy.rhc.vo.TaskSubmitPVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 任务进度状态切面
 */
@Slf4j
@Aspect
@Component
public class ProcessPointAdvice {

    @Autowired
    private CacheUtil cacheUtil;

    @Around("@annotation(processPoint)")
    public Object around(ProceedingJoinPoint joinPoint, ProcessPoint processPoint) throws Throwable {
        String progress = processPoint.progress();
        log.info("{} start...", progress);
        TaskSubmitPVO pvo = Arrays.stream(joinPoint.getArgs())
                .filter(arg -> arg instanceof TaskSubmitPVO)
                .map(arg -> (TaskSubmitPVO) arg)
                .findFirst()
                .orElse(null);
        if (pvo == null || pvo.getInfoConfig() == null) {
            log.error("方法 {} 使用了 @ProcessPoint 注解，但其参数中缺少有效的 TaskSubmitPVO。", joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }

        Info info = pvo.getInfoConfig();
        try {
            info.setStatus(progress, Constants.JOB_STATUS_RUNNING);
            cacheUtil.addInfoToJobList(info);
            log.info("{} begin", progress);
            Object result = joinPoint.proceed();

            info.setStatus(progress, Constants.JOB_STATUS_SUCCESS);
            cacheUtil.addInfoToJobList(info);
            log.info("{} end", progress);
            return result;
        } catch (Throwable e) {
            info.setStatus(progress, Constants.JOB_STATUS_FAIL);
            cacheUtil.addInfoToJobList(info);
            log.error("{} error", progress, e);
            throw e;
        }
    }
}
