package com.example.emos.workflow.db.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TbAmect {
    private Integer id;
    private String uuid;
    private Integer userId;
    private BigDecimal amount;
    private Integer typeId;
    private String reason;
    private String prepayId;
    private Byte status;
    private Date createTime;
}