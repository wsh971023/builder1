package com.cuizhy.rhc.controller.rest;

import com.cuizhy.rhc.cache.CacheUtil;
import com.cuizhy.rhc.global.TaskQueueManager;
import com.cuizhy.rhc.service.WorkService;
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
public class TaskController {

    @Autowired
    private TaskQueueManager taskQueueManager;

    @Autowired
    private WorkService workService;

    @Autowired
    private CacheUtil cacheUtil;

    @RequestMapping("submit")
    public void submitTask(@RequestBody TaskSubmitPVO taskSubmitPVO) {
        taskQueueManager.submitTask(() -> {
            try {
                workService.start(taskSubmitPVO);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @RequestMapping("info")
    public Object info() {
        return cacheUtil.getDb0();
    }
}
