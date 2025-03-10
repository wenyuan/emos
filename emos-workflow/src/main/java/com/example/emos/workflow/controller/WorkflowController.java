package com.example.emos.workflow.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.example.emos.workflow.controller.form.*;
import com.example.emos.workflow.service.WorkflowService;
import com.example.emos.workflow.util.R;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流项目的api接口
 */
@Slf4j
@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    @Resource
    private WorkflowService workflowService;

    @Resource
    private TaskService taskService;

    @Resource
    private ProcessEngine processEngine;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private HistoryService historyService;

    /**
     * 启动会议室申请流程
     */
    @PostMapping("/startMeetingProcess")
    public R startMeetingProcess(@Valid @RequestBody StartMeetingProcessForm form) {
        log.info("工作流，启动会议申请流程，接受请求数据{}", JSONUtil.toJsonPrettyStr(form));
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        param.put("filing", false); // 是否需要贵方
        param.put("type", "会议申请"); // 工作流实例类型
        param.put("createDate", DateUtil.today()); // 启动工作流的日期

        if (form.getGmId() == null) {
            param.put("identity", "总经理");
            param.put("result", "同意"); // 会议状态通过

        } else {
            param.put("identity", "员工");
        }

        String instanceId = workflowService.startMeetingProcess(param);
        return R.ok().put("instanceId", instanceId);

    }

    /**
     * 启动请假流程
     */
    @PostMapping("/startLeaveProcess")
    public R startLeaveProcess(@Valid @RequestBody StartLeaveProcessForm form) {
        log.info("工作流，启动请假流程，接受请求数据{}", JSONUtil.toJsonPrettyStr(form));
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        param.put("filing", false);
        param.put("type", "员工请假");
        param.put("createDate", DateUtil.today());
        String instanceId = workflowService.startLeaveProcess(param);
        return R.ok().put("instanceId", instanceId);
    }

    /**
     * 启动报销流程
     */
    @PostMapping("/startReimProcess")
    public R startReimProcess(@Valid @RequestBody StartReimProcessForm form) {
        log.info("工作流，启动报销流程，接受请求数据{}", JSONUtil.toJsonPrettyStr(form));
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        param.put("filing", false);
        param.put("type", "报销申请");
        param.put("createDate", DateUtil.today());
        param.remove("code");
        String instanceId = workflowService.startReimProcess(param);
        return R.ok().put("instanceId", instanceId);
    }


    /**
     * 审批任务
     */
    @PostMapping("/approvalTask")
    public R approvalTask(@Valid @RequestBody ApprovalTaskForm form) {
        log.info("工作流，审批任务，接受请求数据{}", JSONUtil.toJsonPrettyStr(form));
        HashMap param = new HashMap();
        param.put("taskId", form.getTaskId());
        param.put("approval", form.getApproval());
        workflowService.approvalTask(param);
        return R.ok();
    }

    /**
     * 文件归档
     */
    @PostMapping("/archiveTask")
    public R archiveTask(@Valid @RequestBody ArchiveTaskForm form) {
        log.info("工作流，文件归档接收请求参数为：{}", JSONUtil.toJsonStr(form));
        if (!JSONUtil.isJsonArray(form.getFiles())) {
            return R.error("files不是JSON数组");
        }
        HashMap<String, Object> param = new HashMap<String, Object>() {{
            put("taskId", form.getTaskId());
            put("userId", form.getUserId());
            put("files", JSONUtil.parseArray(form.getFiles()));
        }};
        workflowService.archiveTask(param);
        return R.ok();
    }

    @PostMapping("/deleteProcessById")
    public R deleteProcessById(@Valid @RequestBody DeleteProcessByIdForm form) {
        log.info("工作流，删除流程，接受请求数据{}", JSONUtil.toJsonPrettyStr(form));
        workflowService.deleteProcessById(form.getUuid(), form.getInstanceId(), form.getType(), form.getReason());
        return R.ok();

    }

    @PostMapping("/searchTaskByPage")
    public R searchTaskByPage(@Valid @RequestBody SearchTaskByPageForm form) {
        log.info("工作流，分页查询审批任务，接受请求数据{}", JSONUtil.toJsonPrettyStr(form));
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        param.remove("page");
        param.put("start", start);
        HashMap map = workflowService.searchTaskByPage(param);
        return R.ok().put("page", map);
    }

    /**
     * 查询审批任务详情
     */
    @PostMapping("/searchApprovalContent")
    public R searchApprovalContent(@Valid @RequestBody SearchApprovalContentForm form) {
        log.info("工作流，查询审批意见，接受请求数据{}", JSONUtil.toJsonPrettyStr(form));
        HashMap map = workflowService.searchApprovalContent(
                form.getInstanceId(),
                form.getUserId(),
                form.getRole(),
                form.getType(),
                form.getStatus()
        );
        return R.ok().put("content", map);
    }

    /**
     * 查询bmpn图，根据实例id查询任务，根据任务查询流程定义id
     */
    @PostMapping("/searchApprovalBpmn")
    public void searchApprovalBpmn(@Valid @RequestBody SearchApprovalBpmnForm form, HttpServletResponse response) {
        log.info("工作流，查询Bpmn图，接受请求数据{}", JSONUtil.toJsonPrettyStr(form));
        response.setContentType("image/jpg");
        // 获取执行中的工作流实例中当下的任务，通过该任务可以获取BPMN定义
        Task task = taskService.createTaskQuery().processInstanceId(form.getInstanceId()).singleResult();
        BpmnModel bpmnModel = null;
        List activeActivityIds = null;
        if (task != null) {
            // 获取BPMN流程定义
            bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
            // 获取正在执行中的节点和连线的id
            activeActivityIds = runtimeService.getActiveActivityIds(task.getExecutionId());
        } else {
            // 获取BPMN流程定义
            HistoricTaskInstance taskInstance = historyService.createHistoricTaskInstanceQuery().processInstanceId(form.getInstanceId()).list().get(0);
            bpmnModel = repositoryService.getBpmnModel(taskInstance.getProcessDefinitionId());
            // 因为任务流程已经执行结束，所以没有执行中的节点和连线
            activeActivityIds = new ArrayList<>();
        }

        DefaultProcessDiagramGenerator diagramGenerator = new DefaultProcessDiagramGenerator();
        // 绘制bpmnModel代表的流程的流程图
        String os = System.getProperty("os.name").toLowerCase();
        // 非Windows系统用SimSun字体
        String font = "SimSun";
        // Windows系统用宋体
        if (os.startsWith("win")) {
            font = "宋体";
        }
        try (InputStream in = diagramGenerator.generateDiagram(
                bpmnModel,  // BPMN定义
                "jpg",      // 图片格式
                activeActivityIds,  // 正在执行的节点
                activeActivityIds,  // 正在执行的节点
                font,  // 元素字体
                font,  // 连线字体
                font,  // 图片备注字体
                processEngine.getProcessEngineConfiguration().getProcessEngineConfiguration().getClassLoader(),
                1.0);
             BufferedInputStream bin = new BufferedInputStream(in);
             OutputStream out = response.getOutputStream();
             BufferedOutputStream bout = new BufferedOutputStream(out);
        ) {
            // 把BPMN图片内容写到响应中
            IOUtils.copy(bin, bout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}