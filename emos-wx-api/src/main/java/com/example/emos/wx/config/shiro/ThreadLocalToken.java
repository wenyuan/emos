package com.example.emos.wx.config.shiro;

import org.springframework.stereotype.Component;

/**
 * 作为中间媒介，更新客户端令牌时，在 OAuth2Filter 和 TokenAspect（AOP）间传递数据
 */
@Component
public class ThreadLocalToken {
    private ThreadLocal<String> local = new ThreadLocal<>();

    /**
     * 传递刷新之后的令牌进来
     * @param token
     */
    public void setToken(String token) {
        local.set(token);
    }

    /**
     * 从 ThreadLocal 获取令牌
     * @return
     */
    public String getToken() {
        return local.get();
    }

    public void clear() {
        local.remove();
    }
}
