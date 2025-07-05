/**
 * 任务状态 枚举
 */
const Status = {
    SUCCESS: "success",
    FAIL: "fail",
    RUNNING: "running",
    INIT: "init"
}

const JobProcessMap = {
    jenkins_login: "jenkins登录",
    jenkins_build: "jenkins构建",
    jenkins_download: "构建产物下载",
    git_clone: "Git仓库拉取",
    copy_file: "文件拷贝",
    git_commit_and_push: "Git提交"
}