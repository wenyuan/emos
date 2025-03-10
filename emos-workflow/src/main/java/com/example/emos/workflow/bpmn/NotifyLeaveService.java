package com.example.emos.workflow.bpmn;

import cn.hutool.core.map.MapUtil;
import com.example.emos.workflow.db.dao.TbLeaveDao;
import com.example.emos.workflow.db.dao.TbUserDao;
import com.example.emos.workflow.exception.EmosException;
import com.example.emos.workflow.task.EmailTask;
import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.history.HistoricTaskInstance;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 请假审批工作流服务类
 */
@Component
public class NotifyLeaveService implements JavaDelegate {
    @Resource
    private HistoryService historyService;

    @Resource
    private TbLeaveDao leaveDao;

    @Resource
    private TbUserDao userDao;

    @Resource
    private EmailTask emailTask;

    @Override
    @Transactional
    public void execute(DelegateExecution delegateExecution) {
        String instanceId = delegateExecution.getProcessInstanceId();
        // 查找该任务流中最后一个人的审批任务
        HistoricTaskInstance taskInstance = historyService.createHistoricTaskInstanceQuery().includeProcessVariables()
                .includeTaskLocalVariables().processInstanceId(instanceId)
                .orderByHistoricTaskInstanceEndTime().orderByTaskCreateTime().desc().list().get(0);
        // 获取最后的审批人的审批结果
        String result = taskInstance.getTaskLocalVariables().get("result").toString();
        delegateExecution.setVariable("result", result);

        HashMap param = new HashMap() {{
            put("status", "同意".equals(result) ? 3 : 2);
            put("instanceId", instanceId);
        }};
        // 修改请假状态
        int rows = leaveDao.updateLeaveStatus(param);
        if (rows != 1) {
            throw new EmosException("更新请假记录状态失败");
        }
        if ("同意".equals(result)) {
            // 可以归档
            delegateExecution.setVariable("filing", true);

            /*************************************
             * 给员工发邮件，并且抄送给所在部门经理和所有HR
             *************************************/
            // 获取请假人的userId
            int creatorId = delegateExecution.getVariable("creatorId", Integer.class);
            // 查询请假人的邮箱
            List<String> list_1 = userDao.searchEmailByIds(new int[]{creatorId});
            // 获取部门经理的userId
            int managerId = delegateExecution.getVariable("managerId", Integer.class);
            // 查询请部门经理的邮箱
            ArrayList<String> list_2 = userDao.searchEmailByIds(new int[]{managerId});

            // 查询HR角色对应用户的邮箱
            list_2.addAll(userDao.searchEmailByRoles(new String[]{"HR"}));

            // 创建邮件对象
            SimpleMailMessage email = new SimpleMailMessage();
            String title = delegateExecution.getVariable("title", String.class);
            // 邮件标题
            email.setSubject(title + "已经完成审批");
            String creatorName = delegateExecution.getVariable("creatorName", String.class);
            HashMap map = leaveDao.searchLeaveByInstanceId(instanceId);
            String start = MapUtil.getStr(map, "start");
            String end = MapUtil.getStr(map, "end");
            // 邮件正文
            email.setText("员工" + creatorName + "，于" + start + "至" + end + "的请假申请已经被批准，请及时把请假单签字交给HR归档！");
            // 收件人为请假人
            email.setTo(list_1.toArray(new String[list_1.size()]));
            // 抄送给部门经理和HR
            email.setCc(list_2.toArray(new String[list_2.size()]));
            emailTask.sendAsync(email); // 异步发送邮件
        }
    }
}
