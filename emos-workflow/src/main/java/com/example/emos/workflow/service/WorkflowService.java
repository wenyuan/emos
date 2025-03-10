package com.example.emos.workflow.service;

import java.util.ArrayList;
import java.util.HashMap;

public interface WorkflowService {
    String startMeetingProcess(HashMap param);

    String startLeaveProcess(HashMap param);

    String startReimProcess(HashMap param);

    void approvalTask(HashMap param);

    void archiveTask(HashMap param);

    boolean searchProcessStatus(String instanceId);

    void deleteProcessById(String uuid, String instanceId, String type, String reason);

    ArrayList searchProcessUsers(String instanceId);

    HashMap searchTaskByPage(HashMap param);

    HashMap searchApprovalContent(String instanceId, int userId, String[] role, String type, String status);


}