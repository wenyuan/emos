package com.example.emos.workflow.config.quartz;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.example.emos.workflow.db.pojo.TbAmect;
import com.example.emos.workflow.service.AmectService;
import com.example.emos.workflow.service.AmectTypeService;
import com.example.emos.workflow.service.MeetingService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class MeetingStartJob extends QuartzJobBean {
    @Resource
    private MeetingService meetingService;

    @Transactional
    @Override
    protected void executeInternal(JobExecutionContext ctx) throws JobExecutionException {
        Map map = ctx.getJobDetail().getJobDataMap();
        String uuid = MapUtil.getStr(map, "uuid");

        // 更新会议状态
        HashMap param = new HashMap();
        param.put("status", 4);
        param.put("uuid", uuid);
        meetingService.updateMeetingStatus(param);
        log.debug("会议状态更新成功");

    }

}