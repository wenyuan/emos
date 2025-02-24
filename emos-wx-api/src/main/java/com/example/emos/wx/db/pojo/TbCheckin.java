package com.example.emos.wx.db.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * tb_checkin 记录签到人员、时间、地理坐标
 * @author 
 */
@Data
public class TbCheckin implements Serializable {
    /**
     * 主键
     */
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 签到地址
     */
    private String address;

    /**
     * 国家
     */
    private String country;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区划
     */
    private String district;

    /**
     * 考勤结果
     */
    private Byte status;

    /**
     * 风险等级
     */
    private Integer risk;

    /**
     * 签到日期
     * 数据表中 Date 类型没有时分秒毫秒，Java 中的 Date 类型有时分秒毫秒，这些类型不应该存在，所以用 String 类型来映射
     */
    private String date;

    /**
     * 签到时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}