package com.example.emos.workflow.db.dao;


import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;

@Mapper
public interface TbLeaveDao {
    HashMap searchLeaveByInstanceId(String instanceId);

    int updateLeaveStatus(HashMap param);
}