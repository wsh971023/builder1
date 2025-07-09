package com.cuizhy.rhc.advice;

import com.cuizhy.rhc.model.Info;
import com.cuizhy.rhc.vo.TaskSubmitPVO;

/**
 * 状态更新器
 */
public interface IStatusManager {

    /**
     * 更新状态
     * @param info 任务信息
     * @param progress 进度
     * @param status 状态
     * @param error 错误
     */
    void update(Info info, String progress, String status, String error);

    /**
     * 获取状态信息
     * @return 状态信息
     */
    Object getStatusInfo();

    /**
     * 快速失败
     * @param info
     */
    void failFast(Info info);

    /**
     * 检查任务是否正在运行
     * @param taskSubmitPVO 任务信息
     * @return true: 未运行/已失败,可再次运行  false: 正在运行,不可重复运行
     */
    boolean checkCanReRun(TaskSubmitPVO taskSubmitPVO);
}
