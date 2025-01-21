package com.cuizhy.rhc.model;

import lombok.Data;

@Data
public class Status {

    /**
     * 进程名
     */
    private String process;

    /**
     * 进程状态
     */
    private String status;

    public Status(String process, String status) {
        this.process = process;
        this.status = status;
    }
}
