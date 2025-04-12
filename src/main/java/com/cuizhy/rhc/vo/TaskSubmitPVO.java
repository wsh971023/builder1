package com.cuizhy.rhc.vo;

import com.cuizhy.rhc.model.Info;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交任务 入参
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskSubmitPVO {

    /**
     * 任务名称
     */
    private String work;

    /**
     * 环境
     */
    private String env;

    /**
     * jenkins是否构建
     */
    private String build;

    /**
     * info配置信息
     */
    private Info infoConfig;
}
