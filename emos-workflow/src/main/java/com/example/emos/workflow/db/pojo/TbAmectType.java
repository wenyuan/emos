package com.example.emos.workflow.db.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TbAmectType {
    private Integer id;
    private String type;
    private BigDecimal money;
    private Boolean systemic;
}