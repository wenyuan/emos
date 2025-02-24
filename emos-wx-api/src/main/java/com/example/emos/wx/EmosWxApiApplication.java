package com.example.emos.wx;

import cn.hutool.core.util.StrUtil;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.dao.SysConfigDao;
import com.example.emos.wx.db.pojo.SysConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

@SpringBootApplication
@ServletComponentScan  // 让 XssFilter 类生效
@Slf4j
@EnableAsync // 在 SpringBoot 项目中开启异步多线程
public class EmosWxApiApplication {
    /**
     * 定义持久层（SysConfigDao）的变量引用，因为需要调用持久层的方法
     */
    @Autowired
    private SysConfigDao sysConfigDao;

    /**
     * 定义封装类对象（SystemConstants）的变量引用，因为需要往里面保存数据
     */
    @Autowired
    private SystemConstants constants;

    @Value("${emos.image-folder}")
    private String imageFolder;

    public static void main(String[] args) {
        SpringApplication.run(EmosWxApiApplication.class, args);
    }

    /**
     * 1. 把数据表里保存的考勤时间、区间的设定数据读取出来，然后保存到静态变量中缓存起来
     */
    @PostConstruct  // 注解作用：SpringBoot 启动后的初始化方法
    public void init() {
        List<SysConfig> list = sysConfigDao.selectAllParam();
        list.forEach(one->{
            String key = one.getParamKey();
            // 处理常量名字：转成驼峰命名法（数据库里的是带下划线的），因为要通过反射的方式去找封装对象（SystemConstants）里的变量
            key = StrUtil.toCamelCase(key);
            String value = one.getParamValue();
            try {
                // 通过反射的方式给封装类对象里的变量赋值
                // getDeclaredField() 方法可以根据传入的名字，从封装类里面找变量
                Field field = constants.getClass().getDeclaredField(key);
                // 给封装类里的某个变量赋值
                field.set(constants, value);
            } catch (Exception e){
                log.error("执行异常", e);
            }
        });
        new File(imageFolder).mkdirs();
        //生成测试数据

    }
}
