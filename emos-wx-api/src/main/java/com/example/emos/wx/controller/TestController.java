package com.example.emos.wx.controller;

import com.example.emos.wx.common.util.R;
import com.example.emos.wx.controller.form.TestSayHelloForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/test")
@Api("测试Web接口")
public class TestController {
    @PostMapping("/sayHello")
    @ApiOperation("最简单的测试方法")
    public R sayHello(@Valid @RequestBody TestSayHelloForm form) {
        /* @Valid 对数据进行后端验证
         * @RequestBody 将客户端请求提交的数据封装成对象放到封装类里（来自 SpringMVC）
         */
        return R.ok().put("message", "Hello," + form.getName());
    }

    @GetMapping("/hi")
    public R hi(){
        return R.ok();
    }

    @PostMapping("/addUser")
    @ApiOperation("添加用户")
    @RequiresPermissions(value = {"ROOT","USER:ADD"}, logical = Logical.OR)
    public R addUser() {
        return R.ok("用户添加成功");
    }

}
