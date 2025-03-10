package com.example.emos.workflow.service.impl;

import com.example.emos.workflow.db.dao.TbLeaveDao;
import com.example.emos.workflow.service.LeaveService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;

@Service
public class LeaveServiceImpl implements LeaveService {
    @Resource
    private TbLeaveDao leaveDao;

    @Override
    public HashMap searchLeaveByInstanceId(String instanceId) {
        HashMap map = leaveDao.searchLeaveByInstanceId(instanceId);
        return map;
    }
}
