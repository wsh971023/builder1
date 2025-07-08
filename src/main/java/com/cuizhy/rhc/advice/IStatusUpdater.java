package com.cuizhy.rhc.advice;

import com.cuizhy.rhc.model.Info;

/**
 * 状态更新器
 */
public interface IStatusUpdater {

    /**
     * 更新状态
     * @param info 任务信息
     * @param progress 进度
     * @param status 状态
     * @param error 错误
     */
    void update(Info info, String progress, String status, Throwable error);

    /**
     * 获取状态信息
     * @return 状态信息
     */
    Object getStatusInfo();
}
