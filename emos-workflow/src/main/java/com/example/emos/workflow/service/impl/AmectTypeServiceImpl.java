package com.example.emos.workflow.service.impl;

import com.example.emos.workflow.db.dao.TbAmectTypeDao;
import com.example.emos.workflow.service.AmectTypeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;

@Service
public class AmectTypeServiceImpl implements AmectTypeService {

    @Resource
    private TbAmectTypeDao amectTypeDao;

    @Override
    public HashMap searchByType(String type) {
        HashMap map = amectTypeDao.searchByType(type);
        return map;
    }
}
