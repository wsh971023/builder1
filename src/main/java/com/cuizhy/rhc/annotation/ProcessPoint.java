package com.cuizhy.rhc.annotation;

import com.cuizhy.rhc.enums.TaskPhase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义切面注解 用于任务状态进度标记
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProcessPoint {

    /**
     * 定义当前任务的进度常量
     * 例如：Constants.JOB_PROGRESS_JENKINS_LOGIN
     * @return 任务进度标识
     */
    String progress();

    /**
     * 定义此方法在任务生命周期中的阶段。
     * 默认为 WRAP，以兼容旧的、简单的用法。
     */
    TaskPhase phase() default TaskPhase.WRAP;
}
