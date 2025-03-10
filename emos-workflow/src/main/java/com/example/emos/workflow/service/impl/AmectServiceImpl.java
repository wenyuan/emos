package com.example.emos.workflow.service.impl;

import com.example.emos.workflow.db.dao.TbAmectDao;
import com.example.emos.workflow.db.pojo.TbAmect;
import com.example.emos.workflow.service.AmectService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AmectServiceImpl implements AmectService {

    @Resource
    private TbAmectDao amectDao;

    @Override
    public int insert(TbAmect amect) {
        int rows = amectDao.insert(amect);
        return rows;
    }
}
