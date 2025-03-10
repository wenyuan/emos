package com.example.emos.workflow.task;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.Serializable;

@Component
public class EmailTask implements Serializable {
    @Resource
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String mailbox;

    @Async
    public void sendAsync(SimpleMailMessage message) {
        message.setFrom(mailbox);
        javaMailSender.send(message);
    }
}
