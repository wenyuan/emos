package com.example.emos.workflow.service.impl;

import com.example.emos.workflow.db.dao.TbReimDao;
import com.example.emos.workflow.service.ReimService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;

@Service
public class ReimServiceImpl implements ReimService {
    @Resource
    private TbReimDao reimDao;

    @Override
    public HashMap searchReimByInstanceId(String instanceId) {
        HashMap map = reimDao.searchReimByInstanceId(instanceId);
        return map;
    }
}
