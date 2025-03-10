package com.example.emos.workflow.config.quartz;

import com.example.emos.workflow.service.MeetingService;
import com.example.emos.workflow.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 会议开始时执行：检查工作流的会议审批任务
 */
@Slf4j
@Component
public class MeetingWorkflowJob extends QuartzJobBean {
    @Resource
    private RuntimeService runtimeService;

    @Resource
    private MeetingService meetingService;

    @Resource
    private HistoryService historyService; // 执行工作流产生的历史记录

    @Override
    protected void executeInternal(JobExecutionContext ctx) throws JobExecutionException {
        // 获取创建定时器时候传入的参数
        Map map = ctx.getJobDetail().getJobDataMap();
        // 获取会议UUID字符串
        String uuid = map.get("uuid").toString();
        // 获取工作流实例ID
        String instanceId = map.get("instanceId").toString();
        // 判断会议审批是不是未结束
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
        if (instance != null) {
            map.put("processStatus", "未结束");

            // 删除工作流和相关的定时器
            runtimeService.deleteProcessInstance(instanceId, "审批过期");
            long count = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceId).count();
            if (count > 0) {
                // 删除执行工作流产生的历史记录
                historyService.deleteHistoricProcessInstance(instanceId);
            }

            // 封装要更新的状态
            HashMap param = new HashMap();
            param.put("uuid", uuid);
            // 会议状态为已拒绝
            param.put("status", 2);
            // 更新会议状态
            meetingService.updateMeetingStatus(param);
            log.debug("会议已失效");
        }
    }
}