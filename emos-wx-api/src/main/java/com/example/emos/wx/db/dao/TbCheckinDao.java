package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbCheckin;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 查询当天是否已签到
 */
@Mapper
public interface TbCheckinDao {
    /**
     * 查询当前日期某个用户在某个时间段内有没有签到记录
     */
    public Integer haveCheckin(HashMap param);

    /**
     * 保存签到数据
     */
    public void insert(TbCheckin checkin);

    /**
     * 查询指定员工今天的签到结果（含用户的基本信息也一起查出来）
     */
    public HashMap searchTodayCheckin(int userId);

    /**
     * 统计指定员工总的签到天数
     */
    public long searchCheckinDays(int userId);

    /**
     * 查询指定员工的本周签到情况（需要考虑特殊节假日的情况）
     */
    public ArrayList<HashMap> searchWeekCheckin(HashMap param);
}
