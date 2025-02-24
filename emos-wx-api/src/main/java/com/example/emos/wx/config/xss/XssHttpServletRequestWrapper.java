package com.example.emos.wx.config.xss;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;


// HttpServletRequestWrapper 类使用了装饰器模式，封装了各服务器厂商（比如 Tomcat）的 Request 实现类
// 只需要覆盖 Wrapper 的方法，就能做到给请求对象增加新功能
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    // 默认是空构造器，无法接收传入的请求对象，故而手动声明一个构造器，接收请求对象，然后直接调用父类构造器即可
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    // 所有从请求里获取数据的方法，都需要覆盖，实现把返回结果进行转义
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if(!StrUtil.hasEmpty(value)){
            // HtmlUtil.filter() 实现转义的功能
            value = HtmlUtil.filter(value);
        }
        return value;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if(values != null){
            for (int i=0; i<values.length; i++){
                String value = values[i];
                if(!StrUtil.hasEmpty(value)){
                    value = HtmlUtil.filter(value);
                }
                values[i] = value;
            }
        }
        return values;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameters = super.getParameterMap();
        // 建立一个新的 Map 对象，存放转义后的数据
        // 使用 LinkedHashMap 保持插入时的顺序
        LinkedHashMap<String, String[]> map = new LinkedHashMap();
        if(parameters != null){
            for (String key: parameters.keySet()) {
                String[] values = parameters.get(key);
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    if (!StrUtil.hasEmpty(value)) {
                        value = HtmlUtil.filter(value);
                    }
                    values[i] = value;
                }
                map.put(key, values);
            }
        }
        return map;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (!StrUtil.hasEmpty(value)) {
            value = HtmlUtil.filter(value);
        }
        return value;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 从请求中读取 IO 流
        InputStream in = super.getInputStream();
        // 读取请求 IO 流里的数据
        InputStreamReader reader =new InputStreamReader(in, Charset.forName("UTF-8"));
        BufferedReader buffer =new BufferedReader(reader);
        StringBuffer body = new StringBuffer();
        String line = buffer.readLine();
        while(line != null){
            body.append(line);
            line = buffer.readLine();
        }
        buffer.close();
        reader.close();
        in.close();
        // 客户端提交的数据是 JSON，读取出来的内容是一个 JSON 数据，Java不支持原生 JSON 格式，需要将其转换成 Map 对象
        Map<String,Object> map = JSONUtil.parseObj(body.toString());
        // 建立一个新的 Map 对象，存放转义后的数据
        Map<String,Object> result = new LinkedHashMap<>();
        for(String key: map.keySet()) {
            Object val = map.get(key);
            if (val instanceof String) {
                if (!StrUtil.hasEmpty(val.toString())) {
                    result.put(key, HtmlUtil.filter(val.toString()));
                }
            } else {
                result.put(key, val);
            }
        }
        // 重新转成 JSON 字符串，再创建一个 IO 流从中读数据，以实现最后仍旧返回 IO 流
        String json = JSONUtil.toJsonStr(result);
        ByteArrayInputStream bain = new ByteArrayInputStream(json.getBytes());
        // 构建要求返回的 IO 流对象（ServletInputStream）
        // 使用匿名内部类（ServletInputStream 是个抽象类，需要实现里面所有的方法）
        return new ServletInputStream() {
            // 仅需要覆盖这个方法
            @Override
            public int read() throws IOException {
                return bain.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }
}
