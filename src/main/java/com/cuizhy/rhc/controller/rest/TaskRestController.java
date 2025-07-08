package com.cuizhy.rhc.controller.rest;

import com.cuizhy.rhc.advice.IStatusManager;
import com.cuizhy.rhc.cache.CacheUtil;
import com.cuizhy.rhc.global.TaskQueueManager;
import com.cuizhy.rhc.service.WorkflowService;
import com.cuizhy.rhc.vo.TaskSubmitPVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多任务控制器
 */
@RestController
@RequestMapping("api/task")
public class TaskRestController {

    @Autowired
    private TaskQueueManager taskQueueManager;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private IStatusManager statusManager;

    @RequestMapping("submit")
    public void submitTask(@RequestBody TaskSubmitPVO taskSubmitPVO) {
        if (!statusManager.checkCanReRun(taskSubmitPVO)){
            throw new RuntimeException("任务正在运行中，请勿重复提交！");
        }
        taskQueueManager.submitTask(() -> {
            try {
                workflowService.start(taskSubmitPVO);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @RequestMapping("info")
    public Object info() {
        return statusManager.getStatusInfo();
    }
}
