package com.example.emos.workflow;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableAsync;

import java.awt.*;
import java.io.InputStream;

@SpringBootApplication
@EnableAsync
@Slf4j
@ComponentScan("com.example.*")
@MapperScan("com.example.emos.workflow.db.dao")
@ServletComponentScan
public class EmosWorkflowApplication {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(EmosWorkflowApplication.class, args);
        Resource resource = ctx.getResource("classpath:font/simsun.ttc");

        try (InputStream in = resource.getInputStream()) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (Exception e) {
            log.error("执行异常", e);
        }
    }

}