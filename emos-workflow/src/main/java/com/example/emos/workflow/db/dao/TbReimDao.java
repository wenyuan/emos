package com.example.emos.workflow.db.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;

@Mapper
public interface TbReimDao {
    HashMap searchReimByInstanceId(String instanceId);

    int updateReimStatus(HashMap param);
}