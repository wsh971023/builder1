package com.cuizhy.rhc.global;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
public class TaskQueueManager {

    /**
     * 先进先出队列
     */
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();

    private final ExecutorService executorService;

    public TaskQueueManager() {
        this.executorService = Executors.newFixedThreadPool(4);
        startConsumers();
    }

    /**
     * 启动消费者
     */
    private void startConsumers(){
        for (int i = 0; i < 4; i++) {
            executorService.submit(() -> {
                while (true) {
                    try {
                        Runnable task = taskQueue.take();
                        task.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Task Consumer execute error",e);
                    }
                }
            });
        }
    }

    /**
     * 提交任务
     * @param task 任务
     */
    public void submitTask(Runnable task){
        boolean result = taskQueue.offer(task);
        if (!result){
            log.error("Task Queue is full");
            throw new RuntimeException("Task Queue is full");
        }
    }

    /**
     * 关闭队列
     */
    public void shutdown(){
        executorService.shutdown();
    }
}
