package com.example.emos.workflow.db.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;

@Mapper
public interface TbAmectTypeDao {
    HashMap searchByType(String type);
}