package com.cuizhy.rhc.model;

import com.cuizhy.rhc.constants.Constants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 工程及仓库配置信息
 */
@Slf4j
@Data
public class Info {

  /**
   * 主键
   */
  private Integer id;

  /**
   * 工程名称
   */
  private String name;

  /**
   * 仓库地址
   */
  private String repoUrl;

  /**
   * 分支
   */
  private String branch;

  /**
   * 文件路径
   */
  private String filepath;

  /**
   * 是否前端工程
   */
  private Integer isFront;

  /**
   * 构建任务名称
   */
  private String jobName;

  /**
   * 环境
   */
  private String env;

  /**
   * 状态信息
   */
  List<Status> statusInfo = new ArrayList<>();

  public Info() {
    Status jenkins_login = new Status(Constants.JOB_PROGRESS_JENKINS_LOGIN, Constants.JOB_STATUS_INIT);
    Status jenkins_build = new Status(Constants.JOB_PROGRESS_JENKINS_BUILD, Constants.JOB_STATUS_INIT);
    Status jenkins_download = new Status(Constants.JOB_PROGRESS_JENKINS_DOWNLOAD, Constants.JOB_STATUS_INIT);
    Status git_clone = new Status(Constants.JOB_PROGRESS_GIT_CLONE, Constants.JOB_STATUS_INIT);
    Status copy_file = new Status(Constants.JOB_PROGRESS_COPY_FILE, Constants.JOB_STATUS_INIT);
    Status git_commit_and_push = new Status(Constants.JOB_PROGRESS_GIT_COMMIT_AND_PUSH, Constants.JOB_STATUS_INIT);
    statusInfo.add(jenkins_login);
    statusInfo.add(jenkins_build);
    statusInfo.add(jenkins_download);
    statusInfo.add(git_clone);
    statusInfo.add(copy_file);
    statusInfo.add(git_commit_and_push);
  }

  public void setStatus(String process, String status) {
    if (statusInfo == null) {
      log.warn("statusInfo 列表为空，无法设置状态");
      return;
    }

    Optional<Status> optionalStatus = statusInfo.stream()
            .filter(s -> s.getProcess().equalsIgnoreCase(process))
            .findFirst();

    if (optionalStatus.isPresent()) {
      optionalStatus.get().setStatus(status);
    } else {
      log.warn("未找到 process 为 {} 的 Status 对象", process);
    }
  }

  public Status getStatus(String process) {
    if (statusInfo == null) {
      log.warn("statusInfo 列表为空，无法获取状态");
      return null;
    }

    Optional<Status> optionalStatus = statusInfo.stream()
            .filter(s -> s.getProcess().equalsIgnoreCase(process))
            .findFirst();

    return optionalStatus.orElse(null);
  }
}
