package com.example.emos.wx.config.shiro;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * 把 token 封装成认证对象
 * 用途：客户端提交的 token 不能直接交给 Shiro 框架，需要先封装成 AuthenticationToken 类型的对象
 */
public class OAuth2Token implements AuthenticationToken {
    private String token;

    // 带参构造器
    public OAuth2Token(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
