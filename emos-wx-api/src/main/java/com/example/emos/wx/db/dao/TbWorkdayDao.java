package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbWorkday;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbWorkdayDao {
    public Integer searchTodayIsWorkday();

    /**
     * 查询指定范围内的特殊工作日
     */
    public ArrayList<String> searchWorkdayInRange(HashMap param);
}