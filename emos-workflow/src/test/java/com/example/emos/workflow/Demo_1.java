package com.example.emos.workflow;

import org.activiti.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class Demo_1 {
    @Resource
    private RuntimeService runtimeService;

    @Test
    void run() {
        runtimeService.startProcessInstanceByKey("Process_1741250147079").getProcessInstanceId();
    }
}
