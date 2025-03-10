package com.example.emos.workflow.bpmn;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
//import com.example.emos.workflow.config.quartz.MeetingRoomJob;
import com.example.emos.workflow.config.quartz.MeetingStartJob;
import com.example.emos.workflow.config.quartz.MeetingEndJob;
//import com.example.emos.workflow.config.quartz.MeetingStatusJob;
import com.example.emos.workflow.config.quartz.QuartzUtil;
import com.example.emos.workflow.service.MeetingService;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 会议申请工作流服务类
 */
@Slf4j
@Component
public class NotifyMeetingService implements JavaDelegate {
    @Resource
    private QuartzUtil quartzUtil;

    @Resource
    private MeetingService meetingService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        // 获取全局变量
        Map map = delegateExecution.getVariables();
        String uuid = MapUtil.getStr(map, "uuid"); // 会议uuid
        String url = MapUtil.getStr(map, "url");
        // 审批结果
        String result = MapUtil.getStr(map, "result");

        HashMap data = meetingService.searchMeetingByUUID(uuid);
        String title = MapUtil.getStr(data, "title");
        String date = MapUtil.getStr(data, "date");
        String start = MapUtil.getStr(data, "start");
        String end = MapUtil.getStr(data, "end");
        /*
         * 不管审批结果是说明，只要审批任务已经结束，就没必要保留检查工作流状态的定时器
         * 根据分组名称和定时器ID找到定时器，并且执行删除
         */
        quartzUtil.deleteJob(uuid, "会议工作流组");

        if (result.equals("同意")) {
            // 更新会议状态为3
            meetingService.updateMeetingStatus(new HashMap() {{
                put("uuid", uuid);
                put("status", 3);
            }});
            // 设置会议开始定时器
            JobDetail jobDetail = JobBuilder.newJob(MeetingStartJob.class).build();
            map = jobDetail.getJobDataMap();
            map.put("uuid", uuid); // 会议uuid
            Date executeDate = DateUtil.parse(date + " " + start, "yyyy-MM-dd HH:mm");
            quartzUtil.addJob(jobDetail, uuid, "会议开始任务组", executeDate);

            // 设置会议结束定时器
            jobDetail = JobBuilder.newJob(MeetingEndJob.class).build();
            map = jobDetail.getJobDataMap();
            map.put("uuid", uuid); // 会议uuid
            map.put("title", title);
            map.put("date", date);
            map.put("start", start);
            map.put("end", end);
            executeDate = DateUtil.parse(date + " " + end, "yyyy-MM-dd HH:mm");
            quartzUtil.addJob(jobDetail, uuid, "会议结束任务组", executeDate);

        } else {
            meetingService.updateMeetingStatus(new HashMap() {{
                put("uuid", uuid);
                put("status", 2);
            }});
        }

        JSONObject json = new JSONObject();
        json.set("result", result);
        json.set("uuid", uuid);
        String processId = delegateExecution.getProcessInstanceId();
        json.set("processId", processId);
        try {
            HttpResponse response = HttpRequest.post(url).header("Content-Type", "application/json").body(json.toString()).execute();
            log.debug(response.body());
        } catch (Exception e) {
            log.error("发送通知失败", e);
        }

    }
}