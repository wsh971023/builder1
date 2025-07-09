package com.cuizhy.rhc.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {

    /**
     * 将 Throwable 转换为标准日志格式字符串，等价于 e.printStackTrace()
     */
    public static String getStackTrace(Throwable throwable) {
        if (throwable == null) return "";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
