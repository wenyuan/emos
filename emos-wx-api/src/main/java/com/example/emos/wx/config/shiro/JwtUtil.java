package com.example.emos.wx.config.shiro;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    // 密钥
    @Value("${emos.jwt.secret}")  // 值注入
    private String secret;

    // 过期时间（天）
    @Value("${emos.jwt.expire}")
    private int expire;

    // 根据 userId 创建 token
    public String createToken(int userId){
        // 计算当前时间后 ${emos.jwt.expire} 的日期
        Date date=DateUtil.offset(new Date(), DateField.DAY_OF_YEAR,5);
        // 创建加密算法对象
        Algorithm algorithm = Algorithm.HMAC256(secret);
        // 创建内部类对象来生成 token
        JWTCreator.Builder builder = JWT.create();
        // 绑定 userId -> 设置过期时间 -> 设置密钥算法
        String token = builder.withClaim("userId",userId).withExpiresAt(date).sign(algorithm);
        return token;
    }

    // 从 token 解密出 userId
    public int getUserId(String token) {
        DecodedJWT jwt = JWT.decode(token);
        int userId=jwt.getClaim("userId").asInt();
        return userId;
    }

    // 验证 token
    public void verifierToken(String token) {
        // 创建算法对象
        Algorithm algorithm = Algorithm.HMAC256(secret);
        // 创建验证对象
        JWTVerifier verifier = JWT.require(algorithm).build();
        // 调用验证方法，如果验证失败会抛出一个 RuntimeException
        verifier.verify(token);
    }
}
