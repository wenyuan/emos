package com.example.emos.workflow.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.example.emos.workflow.config.quartz.MeetingWorkflowJob;
import com.example.emos.workflow.config.quartz.QuartzUtil;
import com.example.emos.workflow.service.LeaveService;
import com.example.emos.workflow.service.MeetingService;
import com.example.emos.workflow.service.ReimService;
import com.example.emos.workflow.service.WorkflowService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class WorkflowServiceImpl implements WorkflowService {
    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private HistoryService historyService;

    @Resource
    private QuartzUtil quartzUtil;

    @Resource
    private MeetingService meetingService;

    @Resource
    private LeaveService leaveService;

    @Resource
    private ReimService reimService;

    @Override
    public String startMeetingProcess(HashMap param) {
        // 启动工作流（"meeting"是工作流的ID）
        String instanceId = runtimeService.startProcessInstanceByKey("meeting", param).getProcessInstanceId();
        String uuid = param.get("uuid").toString();
        String date = param.get("date").toString();
        String start = param.get("start").toString();

        /*
         * 创建定时器，执行时间为会议开始时间。
         * 如果会议开始前，该会议还没有审批通过，定时器就把会议状态更新成2，然后关闭工作流；
         * 如果会议审批通过，需要删除这个定时器任务
         */
        JobDetail jobDetail = JobBuilder.newJob(MeetingWorkflowJob.class).build();
        Map dataMap = jobDetail.getJobDataMap();
        // 给定时任务传入的参数：会议UUID和工作流实例ID
        dataMap.put("uuid", uuid);
        dataMap.put("instanceId", instanceId);
        // 定时器在会议开始的时候执行
        Date executeDate = DateUtil.parse(date + " " + start, "yyyy-MM-dd HH:mm:ss");
        // 创建定时器，检查工作流审批状态，参数：定时器要执行的任务jobDetail，定时器的名称（拿会议的UUID作名称），分组名称，日期对象
        quartzUtil.addJob(jobDetail, uuid, "会议工作流组", executeDate);

        return instanceId;
    }

    @Override
    public String startLeaveProcess(HashMap param) {
        String instanceId = runtimeService.startProcessInstanceByKey("leave", param).getProcessInstanceId(); //启动工作流
        return instanceId;
    }

    @Override
    public String startReimProcess(HashMap param) {
        String instanceId = runtimeService.startProcessInstanceByKey("reim", param).getProcessInstanceId(); //启动工作流
        return instanceId;
    }


    /**
     * 审批流程
     */
    @Override
    public void approvalTask(HashMap param) {
        String taskId = MapUtil.getStr(param, "taskId");
        String approval = MapUtil.getStr(param, "approval"); // 审批结果
        taskService.setVariableLocal(taskId, "result", approval);
        taskService.complete(taskId);
    }

    @Override
    public void archiveTask(HashMap param) {
        String taskId = MapUtil.getStr(param, "taskId");
        int userId = MapUtil.getInt(param, "userId");
        JSONArray files = (JSONArray) param.get("files"); // 请假单照片是字符串，需要转成数组
        // 把归档文件信息存储在工作流实例中，将来查看审批记录的时候能看到归档文件
        taskService.setVariable(taskId, "files", files);
        // 归档执行结束，filing设置为false
        taskService.setVariable(taskId, "filing", true);
        // 设置归档任务的归属人变为具体用户，而不是HR角色，这样其他HR员工就无法看到该任务
        taskService.setOwner(taskId, userId + "");
        // 当前用户认领该审批任务
        taskService.setAssignee(taskId, userId + "");
        // 执行归档
        taskService.complete(taskId);
    }


    @Override
    public boolean searchProcessStatus(String instanceId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(instanceId)
                .singleResult();
        return instance == null;
    }

    /**
     * 删除流程实例，同时删除流程历史信息
     */
    @Override
    public void deleteProcessById(String uuid, String instanceId, String type, String reason) {
        long count = runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).count();
        if (count > 0) {
            // 删除工作流
            runtimeService.deleteProcessInstance(instanceId, reason);
        }
        count = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceId).count();
        if (count > 0) {
            // 删除工作流历史
            historyService.deleteHistoricProcessInstance(instanceId);
        }
        // 判断是否是会议工作流，然后删除定时器
        if (type.equals("会议申请")) {
            quartzUtil.deleteJob(uuid, "会议开始任务组");
            quartzUtil.deleteJob(uuid, "会议结束任务组");
            quartzUtil.deleteJob(uuid, "会议工作流组");
        }


    }

    @Override
    public ArrayList searchProcessUsers(String instanceId) {
        List<HistoricTaskInstance> taskList = historyService.createHistoricTaskInstanceQuery().processInstanceId(instanceId).finished().list();
        ArrayList<String> list = new ArrayList<>();
        taskList.forEach(one -> {
            list.add(one.getAssignee());
        });
        return list;
    }

    @Override
    public HashMap searchTaskByPage(HashMap param) {
        ArrayList<Map> list = new ArrayList();
        int userId = MapUtil.getInt(param, "userId");
        JSONArray role = JSONUtil.parseArray(param.get("role"));
        int page = MapUtil.getInt(param, "page");
        int start = MapUtil.getInt(param, "start");
        int length = MapUtil.getInt(param, "length");
        String status = MapUtil.getStr(param, "status");
        String creatorName = MapUtil.getStr(param, "creatorName");
        String type = MapUtil.getStr(param, "type");
        String instanceId = MapUtil.getStr(param, "instanceId");
        Long totalCount = 0L;
        List<String> assignee = new ArrayList();
        // 把当前用户加入审批人列表，用于查询任务
        assignee.add(userId + "");
        // 把当前用户的角色添加到审批人列表，因为有的审批任务关联的是角色，比如财务审批或者HR
        role.forEach(one -> {
            assignee.add(one.toString());
        });

        if ("待审批".equals(status)) {
            // 创建查询对象
            TaskQuery taskQuery = taskService.createTaskQuery().orderByTaskCreateTime().desc()
                    .includeProcessVariables().includeTaskLocalVariables().taskAssigneeIds(assignee);
            // 由于某个审批人可能会有很多待审批的任务，为了能缩小查询范围，可以设置更多查询条件
            if (StrUtil.isNotBlank(creatorName)) {
                // 可以根据创建者名称缩小查询范围
                taskQuery.processVariableValueEquals("creatorName", creatorName);
            }
            if (StrUtil.isNotBlank(type)) {
                // 可以根据类型缩小查询范围
                taskQuery.processVariableValueEquals("type", type);
            }
            if (StrUtil.isNotBlank(instanceId)) {
                // 可以根据工作流实例缩小查询范围
                taskQuery.processInstanceId(instanceId);
            }
            totalCount = taskQuery.count();
            List<Task> taskList = taskQuery.listPage(start, length);
            for (Task task : taskList) {
                // 把工作流实例的全局变量克隆一份，避免往里面添加东西自动变成全局范围
                Map<String, Object> map = task.getProcessVariables();
                map.put("processId", task.getProcessInstanceId());
                map.put("status", status);
                map.put("taskId", task.getId());
                list.add(map);
            }
        } else {
            if ("已审批".equals(status)) {
                HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery()
                        .orderByHistoricTaskInstanceStartTime().desc()
                        .includeTaskLocalVariables().includeProcessVariables()
                        .taskAssigneeIds(assignee).finished().processUnfinished();
                if (StrUtil.isNotBlank(creatorName)) {
                    taskQuery.processVariableValueEquals("creatorName", creatorName);
                }
                if (StrUtil.isNotBlank(type)) {
                    taskQuery.processVariableValueEquals("type", type);
                }
                if (StrUtil.isNotBlank(instanceId)) {
                    taskQuery.processInstanceId(instanceId);
                }
                totalCount = taskQuery.count();
                List<HistoricTaskInstance> taskList = taskQuery.listPage(start, length);
                for (HistoricTaskInstance task : taskList) {
                    Map<String, Object> map = task.getProcessVariables();
                    map.put("processId", task.getProcessInstanceId());
                    map.put("status", status);
                    map.put("taskId", task.getId());
                    list.add(map);
                }
            } else if ("已结束".equals(status)) {
                HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery()
                        .orderByHistoricTaskInstanceStartTime().desc()
                        .includeTaskLocalVariables().includeProcessVariables()
                        .taskAssigneeIds(assignee).finished().processFinished();
                List<HistoricTaskInstance> taskList = taskQuery.listPage(start, length);
                for (HistoricTaskInstance task : taskList) {
                    Map<String, Object> map = task.getProcessVariables();
                    map.put("processId", task.getProcessInstanceId());
                    map.put("status", status);
                    map.put("taskId", task.getId());
                    list.add(map);
                }
            }
        }
        HashMap map = new HashMap();
        map.put("list", list);
        map.put("totalCount", totalCount);
        map.put("pageIndex", start);
        map.put("pageSize", length);
        return map;
    }

    @Override
    public HashMap searchApprovalContent(String instanceId, int userId, String[] role, String type, String status) {
        HashMap map = null;
        List<String> assignee = new ArrayList();
        assignee.add(userId + "");
        for (String one : role) {
            assignee.add(one);
        }
        if ("会议申请".equals(type)) {
            map = meetingService.searchMeetingByInstanceId(instanceId);
        } else if ("员工请假".equals(type)) {
            map = leaveService.searchLeaveByInstanceId(instanceId);
        } else if ("报销申请".equals(type)) {
            map = reimService.searchReimByInstanceId(instanceId);
        }
        Map variables; // 保存从工作流实例获取的全局变量
        if (!"已结束".equals(status)) {
            variables = runtimeService.getVariables(instanceId);
        } else {
            HistoricTaskInstance instance = historyService.createHistoricTaskInstanceQuery()
                    .includeTaskLocalVariables().includeProcessVariables().processInstanceId(instanceId).taskAssigneeIds(assignee).processFinished().list().get(0);
            variables = instance.getProcessVariables();
        }
        // 判断全局变量中是否包含文件（URL地址）
        if (variables != null && variables.containsKey("files")) {
            // 把绑定的文件（URL地址）取出来，比如在详情信息面板中显示请假单
            ArrayNode files = (ArrayNode) variables.get("files");
            map.put("files", files);
        }

        // 获取工作流审批结果
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
        if (instance != null) {
            // 如果工作流没有结束，该工作流没有审批结果
            map.put("result", "");
        } else {
            // 工作流审批结果
            map.put("result", variables.get("result"));
        }
        return map;
    }
}