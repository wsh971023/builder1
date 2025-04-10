package com.cuizhy.rhc.constants;

public class Constants {


    public static final String JENKINS_DOWNLOAD_DIR = "jenkins-download";

    /**
     * git 克隆 基础文件夹
     */
    public static final String GIT_CLONE_DIR = "git-repo";

    /**
     * 缓存中正在运行的 job 列表 key
     */
    public static final String JOB_LIST_KEY = "running_job";

    public static final String JENKINS_LOGIN_CACHE_KEY = "jenkins_status";

    public static final String JOB_STATUS_RUNNING = "running";

    public static final String JOB_STATUS_SUCCESS = "success";

    public static final String JOB_STATUS_FAIL = "fail";

    public static final String JOB_STATUS_INIT = "init";

    public static final String JOB_PROGRESS_JENKINS_LOGIN = "jenkins_login";

    public static final String JOB_PROGRESS_JENKINS_BUILD = "jenkins_build";

    public static final String JOB_PROGRESS_JENKINS_DOWNLOAD = "jenkins_download";

    public static final String JOB_PROGRESS_GIT_CLONE = "git_clone";

    public static final String JOB_PROGRESS_COPY_FILE = "copy_file";

    public static final String JOB_PROGRESS_GIT_COMMIT_AND_PUSH = "git_commit_and_push";

    public static final String CONFIG_TYPE_JENKINS = "jenkins";

    public static final String CONFIG_TYPE_GIT = "git";
    public static final String CONFIG_TYPE_PROXY = "proxy";
}
