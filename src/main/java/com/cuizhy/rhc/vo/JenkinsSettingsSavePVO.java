package com.cuizhy.rhc.vo;

import lombok.Data;

/**
 * 保存Jenkins配置VO
 */
@Data
public class JenkinsSettingsSavePVO {

    /**
     * jenkins地址
     */
    private String url;

    /**
     * jenkins用户名
     */
    private String username;

    /**
     * jenkins密码
     */
    private String password;

    /**
     * 触发构建API
     */
    private String build_url;

    /**
     * 构建状态API
     */
    private String build_status_url;

    /**
     * crumb获取API地址
     */
    private String crumb_url;

    /**
     * Job信息API
     */
    private String job_info_url;

    /**
     * 工作空间地址
     */
    private String workspace_url;
}
