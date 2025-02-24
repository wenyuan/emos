package com.example.emos.wx.config;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 这个类作用：
 * 同步收发 RabbitMQ 消息需要用到 ConnectionFactory，所以需要自己创建好 ConnectionFactory 对象然后注册给 Spring 框架
 */
@Configuration
public class RabbitMQConfig {
    @Bean
    public ConnectionFactory getFactory(){
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");  // Linux 主机的 IP 地址
        factory.setPort(5672);         // RabbitMQ 端口号
        return factory;
    }
}
