package com.example.emos.wx.config;

import com.example.emos.wx.exception.EmosException;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice  // 可以全局捕获SpringMVC异常
public class ExceptionAdvice {
    @ResponseBody  // 该方法返回的错误消息需要写到响应里面
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  // 规定响应状态码
    @ExceptionHandler(Exception.class)  // 捕获所有 Exception 子类异常（重要）
    public String exceptionHandler(Exception e) {
        log.error("执行异常", e);
        // MethodArgumentNotValidException 是后端验证失败而抛出的异常，需要精简异常内容
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException exception= (MethodArgumentNotValidException) e;
            // 将错误信息返回给前台（调用方法获取：具体的某些字段因为什么原因没有通过校验）
            return exception.getBindingResult().getFieldError().getDefaultMessage();
        }
        // EmosException，需要精简异常内容
        else if (e instanceof EmosException) {
            EmosException exception= (EmosException) e;
            return exception.getMsg();
        }
        // 未授权异常，定义返回的信息
        else if (e instanceof UnauthorizedException) {
            return "你不具备相关权限";
        }
        // 普通异常，定义返回的信息
        else {
            return "后端执行异常";
        }
    }
}
