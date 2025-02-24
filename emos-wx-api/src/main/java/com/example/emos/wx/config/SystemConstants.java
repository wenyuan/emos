package com.example.emos.wx.config;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 这个封装类用来缓存系统常量数据
 */
@Data
@Component
public class SystemConstants {
    public String attendanceStartTime;
    public String attendanceTime;
    public String attendanceEndTime;
    public String closingStartTime;
    public String closingTime;
    public String closingEndTime;
}
