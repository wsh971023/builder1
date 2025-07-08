package com.cuizhy.rhc.advice;

import com.cuizhy.rhc.annotation.ProcessPoint;
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
import java.util.Optional;

@Slf4j
@Aspect
@Component
public class ProcessPointAdvice {

    @Autowired
    private IStatusUpdater statusUpdater;

    @Around("@annotation(processPoint)")
    public Object trackLifecycle(ProceedingJoinPoint joinPoint, ProcessPoint processPoint) throws Throwable {

        Optional<TaskSubmitPVO> pvoOptional = findTaskSubmitPVO(joinPoint.getArgs());

        if (pvoOptional.isEmpty() || pvoOptional.get().getInfoConfig() == null) {
            log.warn("方法 {} 使用了 @ProcessPoint 注解，但其参数中缺少有效的 TaskSubmitPVO，AOP 跳过。", joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        }

        Info info = pvoOptional.get().getInfoConfig();
        String progress = processPoint.progress();

        switch (processPoint.phase()) {
            case START:
                statusUpdater.update(info, progress, Constants.JOB_STATUS_RUNNING, null);
                try {
                    return joinPoint.proceed();
                }catch (Throwable e) {
                    statusUpdater.update(info, progress, Constants.JOB_STATUS_FAIL, e);
                }


            case END:
                try {
                    Object result = joinPoint.proceed();
                    statusUpdater.update(info, progress, Constants.JOB_STATUS_SUCCESS, null);
                    return result;
                } catch (Throwable e) {
                    statusUpdater.update(info, progress, Constants.JOB_STATUS_FAIL, e);
                    throw e;
                }

            case WRAP:
            default:
                statusUpdater.update(info, progress, Constants.JOB_STATUS_RUNNING, null);
                try {
                    Object result = joinPoint.proceed();
                    statusUpdater.update(info, progress, Constants.JOB_STATUS_SUCCESS, null);
                    return result;
                } catch (Throwable e) {
                    statusUpdater.update(info, progress, Constants.JOB_STATUS_FAIL, e);
                    throw e;
                }
        }
    }

    /**
     * 从方法参数数组中安全地查找 TaskSubmitPVO 实例。
     */
    private Optional<TaskSubmitPVO> findTaskSubmitPVO(Object[] args) {
        if (args == null || args.length == 0) {
            return Optional.empty();
        }
        return Arrays.stream(args)
                .filter(TaskSubmitPVO.class::isInstance)
                .map(TaskSubmitPVO.class::cast)
                .findFirst();
    }
}
