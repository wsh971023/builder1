package com.cuizhy.rhc.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * @author cuizhy
 */
@Setter
@Getter
public class RestVo<T> {

    /**
     * 成功状态
     */
    public final static int SUCCESS = 200;

    /**
     * 错误
     */
    public final static int ERROR = 500;


    /**
     * 重复提交警告
     */
    public final static int RESUBMIT = 401;


    /**
     * 成功类型消息
     */
    public final static String SUCCESS_MESSAGE = "success";


    /**
     * 错误类型消息
     */
    public final static String ERROR_MESSAGE = "server error";

    /**
     * 自定义状态消息
     */
    public final static String CUSTOM_MESSAGE = "";

    /**
     * 重复提交信息
     */
    public final static String RESUBMIT_MESSAGE = "resubmit";



    /**
     * 状态,从上述1-6中选择
     */
    private int code;

    /**
     * 文字消息
     */
    private String message;

    /**
     * 数据对象
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;

    /**
     * 创建默认成功实例
     *
     * @return 成功实例
     */
    public static RestVo<?> newSuccessInstance() {
        return newSuccessInstance(SUCCESS_MESSAGE);
    }

    /**
     * 创建成功实例，自定义消息
     *
     * @param message 消息内容
     * @return 成功实例
     */
    public static RestVo<?> newSuccessInstance(String message) {
        return newSuccessInstance(message, null);
    }

    /**
     * 创建成功实例，数据对象
     *
     * @param data 数据对象
     * @param <T>  数据类型
     * @return 成功实例
     */
    public static <T> RestVo<T> newSuccessInstanceWithData(T data) {
        return newSuccessInstance(SUCCESS_MESSAGE, data);
    }

    /**
     * 创建成功实例，自定义消息和数据对象
     *
     * @param message 消息内容
     * @param data    数据对象
     * @param <T>     数据类型
     * @return 成功实例
     */
    public static <T> RestVo<T> newSuccessInstance(String message, T data) {
        return newInstance(SUCCESS, message, data);
    }

    /**
     * 创建错误实例
     *
     * @return 错误实例
     */
    public static RestVo<?> newErrorInstance() {
        return newErrorInstance(ERROR_MESSAGE);
    }

    /**
     * 创建错误实例，自定义消息
     *
     * @param message 消息内容
     * @return 错误实例
     */
    public static RestVo<?> newErrorInstance(String message) {
        return newInstance(ERROR, message, null);
    }


    /**
     * 创建错误实例，自定义消息和数据对象
     *
     * @param message 消息内容
     * @param data    数据对象
     * @param <T>     数据类型
     * @return 错误实例
     */
    public static <T> RestVo<T> newErrorInstance(String message, T data) {
        return newInstance(ERROR, message, data);
    }


    /**
     * 创建自定义状态实例，数据对象
     *
     * @param data 数据对象
     * @param <T>  数据类型
     * @return 自定义状态实例
     */
    public static <T> RestVo<T> newCustomInstanceWithData(T data) {
        return newCustomInstance(CUSTOM_MESSAGE, data);
    }

    /**
     * 创建自定义状态实例，自定义消息和数据对象
     *
     * @param message 消息内容
     * @param data    数据对象
     * @param <T>     数据类型
     * @return 自定义状态实例
     */
    public static <T> RestVo<T> newCustomInstance(String message, T data) {
        return newInstance(SUCCESS, message, data);
    }

    /**
     * 创建重复提交警告实例
     *
     * @return 重复提交警告实例
     */
    public static RestVo<?> newResubmitInstance() {
        return newResubmitInstance(RESUBMIT_MESSAGE);
    }

    /**
     * 创建重复提交警告实例，自定义消息
     *
     * @param message 消息内容
     * @return 重复提交警告实例
     */
    public static RestVo<?> newResubmitInstance(String message) {
        return newInstance(RESUBMIT, message, null);
    }

    /**
     * 创建重复提交警告实例，数据对象
     *
     * @param data 数据对象
     * @param <T>  数据类型
     * @return 重复提交警告实例
     */
    public static <T> RestVo<T> newResubmitInstanceWithData(T data) {
        return newResubmitInstance(RESUBMIT_MESSAGE, data);
    }

    /**
     * 创建重复提交警告实例，自定义消息和数据对象
     *
     * @param message 消息内容
     * @param data    数据对象
     * @param <T>     数据类型
     * @return 重复提交警告实例
     */
    public static <T> RestVo<T> newResubmitInstance(String message, T data) {
        return newInstance(RESUBMIT, message, data);
    }

    /**
     * 创建实例
     *
     * @param state   状态
     * @param message 消息内容
     * @param data    数据对象
     * @param <T>     数据类型
     * @return RESTful接口返回值对象
     */
    private static <T> RestVo<T> newInstance(int state, String message, T data) {
        RestVo<T> restVo = new RestVo<>();
        restVo.setCode(state);
        restVo.setMessage(message);
        restVo.setData(data);
        return restVo;
    }

}
