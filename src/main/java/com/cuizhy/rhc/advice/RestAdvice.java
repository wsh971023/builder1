package com.cuizhy.rhc.advice;

import com.cuizhy.rhc.vo.RestVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class RestAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        // 判断是否需要处理，true 表示拦截所有返回
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        if (body instanceof String) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(RestVo.newSuccessInstanceWithData(body));
            } catch (Exception e) {
                log.error("convert exception", e);
                return RestVo.newErrorInstance();
            }
        }

        // 如果已经是标准返回格式，直接返回
        if (body instanceof RestVo<?>) {
            return body;
        }
        // 封装成标准返回格式
        return RestVo.newSuccessInstanceWithData(body);
    }

    @ExceptionHandler(Exception.class)
    public RestVo<?> handleException(Exception e) {
        log.error("handle exception", e);
        return RestVo.newErrorInstance();
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // 返回 404 状态码
    public void handleNoResourceFoundException(NoResourceFoundException ex) {
        // 静默处理，无需额外操作
    }
}
