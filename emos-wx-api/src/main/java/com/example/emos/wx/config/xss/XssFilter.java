package com.example.emos.wx.config.xss;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
@WebFilter(urlPatterns = "/*")  // 拦截所有路径下的请求
public class XssFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 我们定义的 Wrapper 接收的参数是 HttpServletRequest 类型的，需要做一下数据类型转换
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);
        // 传入经过装饰器装饰后的请求对象
        filterChain.doFilter(wrapper, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
