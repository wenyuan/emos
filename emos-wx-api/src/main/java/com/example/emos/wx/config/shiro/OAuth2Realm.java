package com.example.emos.wx.config.shiro;

import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.service.UserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 实现认证与授权
 * 用途：在这个类里定义认证与授权的实现方法
 * 必须要继承 AuthorizingRealm 类
 */
@Component
public class OAuth2Realm extends AuthorizingRealm {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    /**
     * 用于判断传入的令牌对象是否符合要求
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof OAuth2Token;
    }

    /**
     * 授权（验证权限时调用）
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection collection) {
        TbUser user = (TbUser) collection.getPrimaryPrincipal();
        int userId = user.getId();
        // 用户权限列表
        Set<String> permsSet = userService.searchUserPermissions(userId);

        // 创建授权对象，然后查询用户的权限列表，把权限列表添加到 info 对象中
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.setStringPermissions(permsSet);
        return info;
    }

    /**
     * 认证（验证登录时调用）
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String accessToken = (String)token.getPrincipal();
        // 从令牌中获取 userId，然后检测该账户是否被冻结
        int userId = jwtUtil.getUserId(accessToken);
        TbUser user = userService.searchById(userId);
        if (user == null) {
            throw new LockedAccountException("账号已被锁定，请联系管理员");
        }
        // 创建认证对象，往 info 对象中添加用户信息，token 字符串
        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, accessToken, getName());
        return info;
    }
}
