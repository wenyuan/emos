package com.example.emos.workflow.db.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;

@Mapper
public interface TbUserDao {
    ArrayList<String> searchEmailByIds(int[] ids);

    ArrayList<String> searchEmailByRoles(String[] roles);
}