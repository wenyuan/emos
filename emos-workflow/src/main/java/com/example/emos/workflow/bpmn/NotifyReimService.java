package com.example.emos.workflow.bpmn;

import cn.hutool.core.map.MapUtil;
import com.example.emos.workflow.db.dao.TbReimDao;
import com.example.emos.workflow.db.dao.TbUserDao;
import com.example.emos.workflow.exception.EmosException;
import com.example.emos.workflow.task.EmailTask;
import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.xml.transform.sax.SAXResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component("notifyReimService")
public class NotifyReimService implements JavaDelegate {

    @Resource
    private HistoryService historyService;

    @Resource
    private TbUserDao userDao;

    @Resource
    private TbReimDao reimDao;

    @Resource
    private EmailTask emailTask;


    @Override
    public void execute(DelegateExecution delegateExecution) {
        // 查找该任务流中最后一个人的审批任务
        HistoricTaskInstance taskInstance = historyService.createHistoricTaskInstanceQuery().includeProcessVariables()
                .includeTaskLocalVariables().processInstanceId(delegateExecution.getProcessInstanceId())
                .orderByHistoricTaskInstanceEndTime().orderByTaskCreateTime().desc().list().get(0);
        // 获取最后的审批人的审批结果
        String result = taskInstance.getTaskLocalVariables().get("result").toString();
        // 把审批结果从局部变量提升为全局变量
        delegateExecution.setVariable("result", result);

        String instanceId = delegateExecution.getProcessInstanceId();
        // 修改报销状态
        HashMap param = new HashMap() {{
            put("status", "同意".equals(result) ? 3:2);
            put("instanceId", instanceId);
        }};

        int rows = reimDao.updateReimStatus(param);
        if (rows != 1) {
            throw new EmosException("更新报销记录状态失败");
        }

        if ("同意".equals(result)) {
            // 可以归档
            delegateExecution.setVariable("filing", true);
            // 查询报销详情信息
            HashMap map = reimDao.searchReimByInstanceId(instanceId);
            String amount = map.get("amout").toString();

            /****************************
             * 给员工发邮件，并且抄送给所在部门经理和所有的财务
             ****************************/
            int creatorId = delegateExecution.getVariable("creatorId", Integer.class);
            List<String> list_1 = userDao.searchEmailByIds(new int[]{creatorId});

            int managerId = delegateExecution.getVariable("managerId", Integer.class);
            ArrayList<String> list_2 = userDao.searchEmailByIds(new int[]{managerId});
            list_2.addAll(userDao.searchEmailByRoles(new String[]{"财务"}));

            SimpleMailMessage email = new SimpleMailMessage();
            String title = delegateExecution.getVariable("title", String.class);
            email.setSubject(title + "已经被批准");
            String creatorName = delegateExecution.getVariable("creatorName", String.class);
            email.setText("员工" + creatorName + "的" + amount + "元报销申请已经通过审批，请及时把报销单交给财务归档！");
            // 邮件收件人为申请者
            email.setTo(list_1.toArray(new String[list_1.size()]));
            // 抄送给部门经理和财务
            email.setCc(list_2.toArray(new String[list_2.size()]));
            // 异步发送邮件
            emailTask.sendAsync(email); //异步发送邮件
        }
    }
}
