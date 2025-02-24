package com.example.emos.wx.config.shiro;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.apache.http.HttpStatus;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 拦截 HTTP 请求，验证 token
 */
@Component
@Scope("prototype")  // Spring 默认创建单例对象，需要将该对象变成多例对象（重要）
public class OAuth2Filter extends AuthenticatingFilter {
    @Autowired
    private ThreadLocalToken threadLocalToken;

    @Value("${emos.jwt.cache-expire}")
    private int cacheExpire;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 拦截请求之后，用于把令牌字符串封装成令牌对象
     */
    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req = (HttpServletRequest) request;
        // 获取请求token
        String token = getRequestToken(req);
        if (StrUtil.isBlank(token)) {
            return null;
        }
        return new OAuth2Token(token);
    }

    /**
     * 拦截请求，判断请求是否需要被 Shiro 处理
     */
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest req = (HttpServletRequest) request;
        // 微信小程序 Ajax 提交 application/json 数据的时候，会先发出 Option 请求用于测试可用性
        // 这里放行 Options 请求，不需要 Shiro 处理
        if (req.getMethod().equals(RequestMethod.OPTIONS.name())) {
            return true;
        }
        // 除了 Options 请求之外，所有请求都要被 Shiro 处理
        return false;
    }

    /**
     * 该方法用于处理所有应该被 Shiro 处理的请求
     */
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        // 允许跨域请求
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));

        threadLocalToken.clear();

        String token = getRequestToken(req);
        if (StrUtil.isBlank(token)) {
            resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
            resp.getWriter().print("无效的令牌");
            // token 无效，直接返回，无需进入 Realm 进行认证和授权判断
            return false;
        }
        try {
            jwtUtil.verifierToken(token);
        } catch (TokenExpiredException e) {
            if (redisTemplate.hasKey(token)) {
                redisTemplate.delete(token);
                int userId = jwtUtil.getUserId(token);
                // 生成新令牌
                token = jwtUtil.createToken(userId);
                // 保存数据
                redisTemplate.opsForValue().set(token, userId+"", cacheExpire, TimeUnit.DAYS);
                threadLocalToken.setToken(token);
            } else {
                resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
                resp.getWriter().print("令牌已过期");
                return false;
            }
        } catch (Exception e) {
            resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
            resp.getWriter().print("无效的令牌");
            return false;
        }
        // 令牌没问题，通过 executeLogin 方法间接让  Shiro 执行 Realm 类
        boolean bool = executeLogin(request, response);
        return bool;
    }

    /**
     * Shiro 判定用户未登录或登陆失败后执行的方法
     */
    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        HttpServletRequest req= (HttpServletRequest) request;
        HttpServletResponse resp= (HttpServletResponse) response;
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
        try{
            resp.getWriter().print(e.getMessage());
        } catch (Exception exception){

        }

        return false;
    }

    /**
     * 掌管拦截请求和返回响应的方法
     */
    @Override
    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest req= (HttpServletRequest) request;
        HttpServletResponse resp= (HttpServletResponse) response;
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        super.doFilterInternal(request, response, chain);

    }

    /**
     * 从请求头中获取令牌
     */
    private String getRequestToken(HttpServletRequest request){
        String token=request.getHeader("token");
        if(StrUtil.isBlank(token)){
            // 获取从请求体中尝试获取令牌
            token=request.getParameter("token");
        }
        return token;
    }
}
