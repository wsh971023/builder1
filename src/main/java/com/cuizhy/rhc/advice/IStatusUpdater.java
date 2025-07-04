package com.cuizhy.rhc.advice;

import com.cuizhy.rhc.model.Info;

public interface IStatusUpdater {
    void update(Info info, String progress, String status, Throwable error);
}
