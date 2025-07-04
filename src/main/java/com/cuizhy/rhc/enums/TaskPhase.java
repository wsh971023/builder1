package com.cuizhy.rhc.enums;

public enum TaskPhase {
    /**
     * 包裹一个完整的同步任务。切面将处理开始、成功、失败所有状态。
     * (默认值)
     */
    WRAP,

    /**
     * 仅标记一个长任务的开始。切面只会将状态设置为 RUNNING。
     */
    START,

    /**
     * 标记一个长任务的结束。切面只会在方法成功或失败时，设置最终状态。
     */
    END
}