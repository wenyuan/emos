package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 用户签到数据：接收小程序提交的签到数据
 */
@Data
@ApiModel
public class CheckinForm {
    private String address;
    private String country;
    private String province;
    private String city;
    private String district;
}
