package com.example.emos.wx.db.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * tb_holidays 本系统默认周六周日为休息日，这张表记录特殊的休息日
 * @author 
 */
@Data
public class TbHolidays implements Serializable {
    /**
     * 主键
     */
    private Integer id;

    /**
     * 日期
     */
    private Date date;

    private static final long serialVersionUID = 1L;
}