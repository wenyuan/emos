package com.example.emos.wx.aop;

import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.ThreadLocalToken;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AOP 切面类，作用：
 * 1. 拦截所有的 Web 方法返回值
 * 2. 判断是否刷新生成新令牌（检查 ThreadLocal 中是否保存令牌，如果有，说明是新生成的，需要把新令牌绑定到 R 对象中）
 */
@Aspect
@Component
public class TokenAspect {

    // 定义媒介类引用的变量
    @Autowired
    private ThreadLocalToken threadLocalToken;

    // 定义切点：拦截 controller 里面所有的 web 方法
    @Pointcut("execution(public * com.example.emos.wx.controller.*.*(..))")
    public void aspect(){

    }

    // 定义环绕事件：web 方法调用之前的参数可以拦截，web 方法返回的结果也可以拦截
    @Around("aspect()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        R r = (R)point.proceed();  // 方法执行结果
        String token = threadLocalToken.getToken();
        // 如果 ThreadLocal 中存在 token，说明是更新的 token
        if (token != null) {
            r.put("token", token);  // 往响应中放置 token
            threadLocalToken.clear();
        }
        return r;
    }
}
