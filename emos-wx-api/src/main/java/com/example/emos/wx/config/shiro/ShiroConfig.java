package com.example.emos.wx.config.shiro;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 作用：把 Fliter 和 Realm 添加到 Shiro 框架
 * 创建四个对象返回给 SpringBoot
 */
@Configuration
public class ShiroConfig {

    /**
     * 用于封装 Realm 对象
     * 注意 securityManager 类的导包路径
     */
    @Bean("securityManager")
    public SecurityManager securityManager(OAuth2Realm realm){
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(realm);
        // 因为我们把登录凭证以令牌的形式保存在客户端，服务端并不保存登录凭证，所以传入 null
        securityManager.setRememberMeManager(null);
        return securityManager;
    }

    /**
     * 用于封装 Filter 对象，设置 Filter 拦截路径
     */
    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager, OAuth2Filter filter) {
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        shiroFilter.setSecurityManager(securityManager);

        // oauth 过滤
        Map<String, Filter> map = new HashMap<>();
        map.put("oauth2", filter);
        shiroFilter.setFilters(map);

        Map<String,String> filterMap = new LinkedHashMap<>();
        // k：请求路径 v：是否拦截
        filterMap.put("/webjars/**", "anon");
        filterMap.put("/druid/**", "anon");
        filterMap.put("/app/**", "anon");
        filterMap.put("/sys/login", "anon");
        filterMap.put("/swagger/**", "anon");
        filterMap.put("/v2/api-docs", "anon");
        filterMap.put("/swagger-ui.html", "anon");
        filterMap.put("/swagger-resources/**", "anon");
        filterMap.put("/captcha.jpg", "anon");
        filterMap.put("/user/register", "anon");
        filterMap.put("/user/login", "anon");
        filterMap.put("/test/**", "anon");
        filterMap.put("/meeting/recieveNotify", "anon");
        // 除了上面的，其他都要拦截（v 和上面定义的 "oauth2" 保持一致）
        filterMap.put("/**", "oauth2");

        shiroFilter.setFilterChainDefinitionMap(filterMap);

        return shiroFilter;

    }

    /**
     * 管理 Shiro 对象生命周期
     */
    @Bean("lifecycleBeanPostProcessor")
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    /**
     * Web 方法执行前，验证权限
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }
}
