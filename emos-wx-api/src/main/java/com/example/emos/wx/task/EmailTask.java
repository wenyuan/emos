package com.example.emos.wx.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 定义线程任务
 */
@Component
@Scope("prototype") // 确保每一个线程任务都是独立的对象，单例的线程任务会出现线程安全问题
public class EmailTask implements Serializable {
    @Autowired
    private JavaMailSender javaMailSender;
    @Value("${emos.email.system}")
    private String mailbox;

    @Async // 异步执行的注解（重要）
    public void sendAsync(SimpleMailMessage message) {
        message.setFrom(mailbox);
        message.setCc(mailbox);  // 抄送一份给发件人，防止被163识别为垃圾邮件
        javaMailSender.send(message);
    }
}
