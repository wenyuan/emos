package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class SearchMessageByPageForm {
    @NotNull
    @Min(1)
    private Integer page;  // 查询第几页数据
    @NotNull
    @Range(min = 1,max = 40)
    private Integer length;  // 这页数据有多少条记录
}
