> 初始化半成品项目简介：基于 SpringBoot 搭建完框架，集成了 Spring，SpringMVC，MyBatis，Swagger，以及 Sa-Token 权限认证框架等等。
>
> 之后只需要往里面添加业务模块和代码即可。

## 创建 SpringBoot 项目

创建 SpringBoot 工程，环境版本如下：

+ JDK 15.0.2
+ SpringBoot 2.5.2
+ MySQL 8.0
+ [Maven 3.6.3](https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.zip)

### 一、把 Tomcat 替换成 Jetty

因为本项目中用到了 WebSocket，而且 Jetty 本身的 IO 就是非阻塞式的，所以在高并发的 WebSocket 环境下，Jetty 比 Tomcat 更加适合。默认情况下，SpringBoot 集成了 Tomcat，所以需要把 Tomcat 替换成 Jetty。

#### 1. 引入 Jetty 依赖库

在 `pom.xml` 文件中，添加 Jetty 依赖库：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

然后要剔除掉 SpringBoot 捆绑的 Tomcat：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!--这里是新加的内容-->
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

#### 2. 配置 YML 文件

```yml
server:
  jetty:
    threads:
      acceptors: 4
      selectors: 8
  port: 8090
  servlet:
    context-path: /emos-api
    multipart:
      max-request-size: 10MB
      max-file-size: 2MB
```

### 二、配置数据库连接

在 `pom.xml` 文件中引入 druid 连接池依赖。因为 Druid 连接池成熟稳定，所以使用这款连接池。

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.1.13</version>
</dependency>
```

在 `application.yml` 中配置 MySQL、Redis 和 MongoDB 的连接信息。

```yml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/emos?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true
      username: root
      password: abc123456
      initial-size: 2
      max-active: 4
      min-idle: 4
      max-wait: 60000
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false

  redis:
    database: 0
    host: localhost
    port: 6379
    password: abc123456
    jedis:
      pool:
        max-active: 1000
        max-wait: -1ms
        max-idle: 16
        min-idle: 8

  data:
    mongodb:
      host: localhost
      port: 27017
      database: emos
      authentication-database: admin
      username: admin
      password: abc123456
```

### 三、配置 MyBatis

本项目在 MySQL 中有四种类型的数据表：工作流数据表、定时器数据表、业务表、系统表。

| 序号 |     类型     |      特征       |          备注          |
| :--: | :----------: | :-------------: | :--------------------: |
|  1   | 工作流数据表 | 以 `act_` 开头  |  存储工作流的各项数据  |
|  2   | 定时器数据表 | 以 `qrtz_` 开头 | 存储 QuartZ 定时器数据 |
|  3   |    业务表    |  以 `tb_` 开头  |      保存业务数据      |
|  4   |    系统表    | 以 `sys_` 开头  |    保存系统配置信息    |

利用 MyBatisX 插件，将所有以 `tb_` 开头的数据表，生成 pojo 类、dao 接口和 XML 文件。然后在 `pom.xml` 文件中，添加配置信息，并且还要注意包名的路径。

```xml
mybatis:
  mapper-locations: classpath*:mapper/*.xml
  type-aliases-package: com.example.emos.wx.db.pojo
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true

logging:
  level:
    root: info
    com.example.emos.wx.db.dao: warn
  pattern:
    console: "%d{yyyy/MM/dd HH:mm:ss}  %-5level  %msg%n"
```

##  集成常用工具库

在 SpringBoot 项目中，经常用到 JSON 转换、发送 HTTP 请求这样的操作，所以要给 Java 项目导入一些常用的工具库。下面导入的是不需要做额外配置的工具库：

### 一、导入基础工具库

| **序号** | **工具库** |      **备注**      |
| :------: | :--------: | :----------------: |
|    1     |   hutool   |     通用工具库     |
|    2     | commons-io | 文件 IO 通用工具库 |
|    3     |   zxing    |   生成二维码图片   |
|    4     |  httpcore  |    HTTP 状态码     |

在 `pom.xml` 文件中添加上述各种依赖库：

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.6.3</version>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpcore</artifactId>
    <version>4.4.13</version>
</dependency>
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.5</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.3.3</version>
</dependency>
```

### 二、定义 R 类

在 `com.example.emos.api.common.util` 包里面的 `R` 类，是一个封装类。它定义了 Web 方法返回给前端的数据格式，包含业务状态码，消息内容，业务数据等。

以后 Controller 中所有的 Web 方法返回的结果都是 R 对象了，Spring 框架会把数据转换成 JSON 格式返回给前端。

```java
package com.example.emos.api.common.util;

import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public class R extends HashMap<String, Object> {
    public R() {
        put("code", HttpStatus.SC_OK);
        put("msg", "success");
    }

    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public static R ok() {
        return new R();
    }

    public static R ok(String msg) {
        R r = new R();
        r.put("msg", msg);
        return r;
    }

    public static R ok(Map<String, Object> map) {
        R r = new R();
        r.putAll(map);
        return r;
    }

    public static R error(int code, String msg) {
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    public static R error(String msg) {
        return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, msg);
    }

    public static R error() {
        return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "未知异常，请联系管理员");
    }
}
```

### 三、整合 SpringDoc

整合 Swagger 时配置类的内容写起来很混乱，尤其是让 Swagger 兼容 JWT 的时候，需要我们配置一长串的链式调用，参考 [SwaggerConfig.java](https://github.com/wenyuan/emos/blob/main/emos-wx-api/src/main/java/com/example/emos/wx/config/SwaggerConfig.java#L24)。

#### 1. 引入 SpringDoc 依赖库

使用 SpringDoc 组件来代替 Swagger，SpringDoc 的功能跟 Swagger 几乎一模一样，但是配置起来非常简单，几个注解就能搞定。

在 `pom.xml` 文件中添加 `SpringDoc` 的依赖库：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-spring-boot-2-webmvc</artifactId>
    <version>3.1.5</version>
</dependency>
```

#### 2. 修改配置文件

在 `application.yml` 文件中定义 SpringDoc 的配置信息：

```yml
springdoc:
  api-docs:
    enabled: true
    path: /doc-api.html
  swagger-ui:
    path: /swagger-ui.html
    disable-swagger-default-url: on
```

#### 3. 编写配置类

SpringDoc 的配置需要我们创建一个配置类，至于类的名字叫什么无所谓。例如在`com.example.emos.api.config` 包里面创建 `SpringDocConfig` 类。

```java
package com.example.emos.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;


@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "emos-api",
                description = "Emos管理系统后端Java项目",
                version = "1.0"
        )
)
@SecurityScheme(
        name = "token",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class SpringDocConfig {

}
```

其中 `@SecurityScheme` 注解定义的是 JWT 部分，也就是说，在 Swagger 页面上可以看到Authorize 按钮，我们可以设置 HTTP 请求头上传的 JWT 令牌。当然了，其中请求头的名字叫做      `token`（本项目用 Sa-Token 权限与认证框架，该框架要求请求头用 `token` 保存令牌），如果后端的 JWT 要求的请求头名字不叫做这个，可以在上面的注解中修改 `name` 属性。

#### 4. 测试 SpringDoc

为了测试 SpringDoc 的效果，可以创建案例测试一下。在` com.example.emos.api.controller` 包中创建 `UserController` 类：

```java
@RestController
@RequestMapping("/user")
@Tag(description = "用户Web接口")
public class UserController {

    @PostMapping("/checCode")
    @Operation(summary = "检测登陆验证码")
    public R checCode(@Valid @RequestBody CheckCodeForm form) {
        return R.ok().put("result", true);
    }
}
```

运行 SpringBoot 项目，然后访问项目的 SpringDoc 页面，网址为 `http://localhost:8080/项目名称/swagger-ui.html`，然后浏览器就能看到内容了。

## 整合权限验证与授权

在 [emos-wx-api](https://github.com/wenyuan/emos/tree/main/emos-wx-api/src/main/java/com/example/emos/wx/config/shiro) 里整合了 Shiro 和 JWT，写了很多的配置信息，而且为了实现 Token 自动续期，还利用上了 ThreadLocal 技术，整个过程比较繁琐。

这里使用了开源的 [Sa-Token](https://github.com/dromara/Sa-Token) 框架。它的权限验证与授权过程，与 Shiro 非常类似，只不过设计的更加简单，直接跟 JWT 融合在了一起。只需写少量的代码就能把 Sa-Token 整合到 SpringBoot 项目中。

Sa-Token 能做的东西很多，具体可以[见官网](https://sa-token.cc/)。

### 一、导入依赖库

在 `pom.xml` 文件中，引入相关的依赖库，作用各不相同：

```xml
<!--核心库-->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot-starter</artifactId>
    <version>1.20.0</version>
</dependency>
<!--用Redis缓存授权信息-->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-dao-redis</artifactId>
    <version>1.20.0</version>
</dependency>
<!--注解式权限验证-->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-aop</artifactId>
    <version>1.20.0</version>
</dependency>
```

### 二、修改配置文件

在 `application.yml` 文件中，定义 Sa-Token 的配置信息：

```yml
Spring: 
	……
  sa-token:
    # token名称 (同时也是cookie名称)
    token-name: token
    # token有效期，单位s 默认30天, -1代表永不过期
    timeout: 2592000
    # token临时有效期 (指定时间内无操作就视为token过期) 单位: 秒
    activity-timeout: -1
    # 是否允许同一账号并发登录 (为true时允许一起登录, 为false时新登录挤掉旧登录)
    allow-concurrent-login: true
    # 在多人登录同一账号时，是否共用一个token (为true时所有登录共用一个token, 为false时每次登录新建一个token)
    is-share: false
    # token风格
    token-style: uuid
```

### 三、权限和角色判定

在本项目中，我们用注解的方式判定用户是否为特定的角色或者拥有某些权限。Sa-Token 框架为我们提供了这样的注解。比如说下面的 Web 方法用到了 `@SaCheckPermission` 注解判断用户是否具备 `ROOT` 或者 `AMECT:INSERT` 权限。

```java
@PostMapping("/insert")
@Operation(summary = "添加罚款记录")
@SaCheckPermission(value = {"ROOT", "AMECT:INSERT"}, mode = SaMode.OR)
public R insert(@Valid @RequestBody InsertAmectForm form) {
	……
}
```

`@SaCheckPermission` 或者 `@SaCheckRole` 注解拦截 HTTP 请求的时候，会调用特定的Java 类来获取用户的权限和角色信息，然后跟注解要求的权限或者角色做匹配，如果能匹配上，就允许 HTTP 请求调用 Web 方法，否则就拒绝 HTTP 请求。

其中，查询用户权限和角色的若干 Java 代码需要我们自己写，下面就先从 SQL 语句开始。

#### 1. 查询用户的权限信息

> 查找已激活的（status=1）指定用户（userId）所拥有的权限名称。

在 `TbUserDao.xml` 文件中，定义 `searchUserPermissions` 这个 SQL 语句：

```xml
    <select id="searchUserPermissions" parameterType="int" resultType="String">
        SELECT DISTINCT p.permission_name
        FROM tb_user u
        JOIN tb_role r ON JSON_CONTAINS(u.role, CAST(r.id AS CHAR))
        JOIN tb_permission p ON JSON_CONTAINS(r.permissions, CAST(p.id AS CHAR))
        WHERE u.id=#{userId} AND u.status=1;
    </select>
```

在 TbUserDao.java 接口中定义 Dao 方法：

```java
@Mapper
public interface TbUserDao {
    ……
    public Set<String> searchUserPermissions(int userId);
}
```

#### 2. 创建 StpInterfaceImpl 类

在 `com.example.emos.api.config` 包中创建 `StpInterfaceImpl.java` 类，这个 Java类就是 Sa-Token 框架拦截 HTTP 请求之后调用的类。在这个类中，我们一共要声明两个方法分别用来查询用户实际的权限和角色。然后 Sa-Token 框架的 `@SaCheckPermission` 或者 `@SaCheckRole` 注解会根据查询出来的权限和角色，跟注解要求的权限或者角色做匹配。

```java
package com.example.emos.api.config;

import cn.dev33.satoken.stp.StpInterface;
import com.example.emos.api.db.dao.TbUserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class StpInterfaceImpl implements StpInterface {
    @Autowired
    private TbUserDao userDao;

    /**
     * 返回一个用户所拥有的权限集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginKey) {
        int userId = Integer.parseInt(loginId.toString());
        Set<String> permissions = userDao.searchUserPermissions(userId);
        ArrayList list = new ArrayList();
        list.addAll(permissions);
        return list;
    }

    /**
     * 返回一个用户所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginKey) {
        //因为本项目不需要用到角色判定，所以这里就返回一个空的ArrayList对象
        ArrayList<String> list = new ArrayList<String>();
        return list;
    }
}
```

至此就已经配置好了 Sa-Token 权限验证框架，比自己动手配置 Shiro+JWT 要简单多了。

## 允许跨域请求

在前后端分离的架构中，允许跨域请求是一个很重要的设置。SpringBoo t项目中允许跨域请求比较简单，只需要我们定义好配置类即可。

在 `com.example.emos.api.config` 包里面创建 `CorsConfig` 类，然后设置允许跨域请求：

```java
package com.example.emos.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH")
                .maxAge(3600);
    }

}
```

## 全局异常

### 一、封装全局异常

在开发项目的过程中，自定义异常也是很常见的，处理异常的时候可以根据异常的类型，判断出哪些是 Java 语言异常，哪些是业务异常。在本项目中，我们也是需要自定义异常类的。

在 `com.example.emos.api.exception` 包中创建 `EmosException.java` 类。这个异常类里面封装了两样东西：异常消息和状态码。

```java
package com.example.emos.api.exception;

import lombok.Data;

@Data
public class EmosException extends RuntimeException {
    private String msg;
    private int code = 500;

    public EmosException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public EmosException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public EmosException(String msg, int code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }

    public EmosException(String msg, int code, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }
}
```

### 二、全局异常处理

在本项目中无论遇到什么样子的异常，都应该集中处理。SpringBoot 提供了集中处理异常的功能，这里我们要加以利用。

在 `com.example.emos.api.config` 包中创建 `ExceptionAdvice.java` 类，SpringBoot 对全局处理异常的要求很简单，我们只要给这个类加上一个 `@RestControllerAdvice` 注解即可。

```java
package com.example.emos.api.config;
import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.json.JSONObject;
import com.example.emos.api.exception.EmosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public String exceptionHandler(Exception e){
        JSONObject json=new JSONObject();
        //处理后端验证失败产生的异常
        if(e instanceof MethodArgumentNotValidException){
            MethodArgumentNotValidException exception= (MethodArgumentNotValidException) e;
            json.set("error",exception.getBindingResult().getFieldError().getDefaultMessage());
        }
        //处理业务异常
        else if(e instanceof EmosException){
            log.error("执行异常",e);
            EmosException exception= (EmosException) e;
            json.set("error",exception.getMsg());
        }
        //处理其余的异常
        else{
            log.error("执行异常",e);
            json.set("error","执行异常");
        }
        return json.toString();
    }
    
    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NotLoginException.class)
    public String unLoginHandler(Exception e){
        JSONObject json=new JSONObject();
        json.set("error",e.getMessage());
        return json.toString();
    }
}
```

## 开启 Java 异步执行

Java 语言是同步执行的，一句代码没执行完，绝不会执行下一句代码。但是在实际的业务流程中，有时候会有一些耗费时间的分支任务。如果把这些琐碎且不重要的任务用异步来执行，那么就可以减轻当前线程的执行压力。

这里说的异步执行就是由 SpringBoot 自动把任务交给线程池中某个线程去执行。在 JavaSE 中我们是自己创建线程分配任务。在 SpringBoot 里任务的分配是更加自动的，具体步骤如下。

### 一、创建线程池

为了把任务分配给其他线程，首先要配置一个线程池出来。`在com.example.emos.api.config` 包中创建 `ThreadPoolConfig.java` 类。

```java
package com.example.emos.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {
    @Bean("AsyncTaskExecutor")
    public AsyncTaskExecutor taskExecutor(){
        ThreadPoolTaskExecutor executor=new ThreadPoolTaskExecutor();
        // 设置核心线程数
        executor.setCorePoolSize(8);
        // 设置最大线程数
        executor.setMaxPoolSize(16);
        // 设置队列容量
        executor.setQueueCapacity(32);
        // 设置线程活跃时间（秒）
        executor.setKeepAliveSeconds(60);
        // 设置默认线程名称
        executor.setThreadNamePrefix("task-");
        // 设置拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 二、开启 SpringBoot 异步执行

在项目的主类 `EmosApiApplication.java` 声明上面加上 `@EnableAsync` 注解即可。等将来我们[发送邮件]()（异步任务）的时候，再去编写任务类，

```java
package com.example.emos.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ServletComponentScan
@Slf4j
@EnableAsync
public class EmosApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmosApiApplication.class, args);
    }

}
```

## 抵御 XSS 攻击

防御 XSS 攻击的思路就是对所有用户的数据先做转义处理，然后再保存到数据库里面。转义之后的信息，将来被加载到网页上面就丧失了作为脚本执行的能力。

### 实现 XSS 内容转义

首先要创建一个执行转义的封装类，这个类继承 `HttpServletRequestWrapper` 父类。

> 在 Web 项目中，我们无法修改 `HttpServletRequest` 实现类的内容，因为请求的实现类是由各个 Web 容器（Tomcat、Jetty等）厂商自己扩展的。

这样我们就可以修改请求类中的内容，修改获取请求数据的函数。

在 `com.example.emos.api.config.xss` 包中创建 `XssHttpServletRequestWrapper.java` 类。把获取请求头和请求体数据的方法都要重写，返回的是经过 XSS 转义后的数据。

```java
package com.example.emos.api.config.xss;

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

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value= super.getParameter(name);
        if(!StrUtil.hasEmpty(value)){
            value=HtmlUtil.cleanHtmlTag(value);
        }
        return value;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values= super.getParameterValues(name);
        if(values!=null){
            for (int i=0;i<values.length;i++){
                String value=values[i];
                if(!StrUtil.hasEmpty(value)){
                    value=HtmlUtil.cleanHtmlTag(value);
                }
                values[i]=value;
            }
        }
        return values;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameters = super.getParameterMap();
        LinkedHashMap<String, String[]> map=new LinkedHashMap();
        if(parameters!=null){
            for (String key:parameters.keySet()){
                String[] values=parameters.get(key);
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    if (!StrUtil.hasEmpty(value)) {
                        value = HtmlUtil.cleanHtmlTag(value);
                    }
                    values[i] = value;
                }
                map.put(key,values);
            }
        }
        return map;
    }

    @Override
    public String getHeader(String name) {
        String value= super.getHeader(name);
        if (!StrUtil.hasEmpty(value)) {
            value = HtmlUtil.cleanHtmlTag(value);
        }
        return value;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        InputStream in= super.getInputStream();
        InputStreamReader reader=new InputStreamReader(in, Charset.forName("UTF-8"));
        BufferedReader buffer=new BufferedReader(reader);
        StringBuffer body=new StringBuffer();
        String line=buffer.readLine();
        while(line!=null){
            body.append(line);
            line=buffer.readLine();
        }
        buffer.close();
        reader.close();
        in.close();
        Map<String,Object> map=JSONUtil.parseObj(body.toString());
        Map<String,Object> result=new LinkedHashMap<>();
        for(String key:map.keySet()){
            Object val=map.get(key);
            if(val instanceof String){
                if(!StrUtil.hasEmpty(val.toString())){
                    result.put(key,HtmlUtil.cleanHtmlTag(val.toString()));
                }
            }
            else {
                result.put(key,val);
            }
        }
        String json=JSONUtil.toJsonStr(result);
        ByteArrayInputStream bain=new ByteArrayInputStream(json.getBytes());
        return new ServletInputStream() {
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
```

接下来我们要创建一个 Filter 类，拦截所有的 HTTP 请求，然后调用上面创建的`XssHttpServletRequestWrapper` 类，这样就能按照我们设定的方式获取请求中的数据了。

在 `com.example.emos.api.config.xss` 包中创建 `XssFilter.java` 类：

```java
package com.example.emos.api.config.xss;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
@WebFilter(urlPatterns = "/*")
public class XssFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request= (HttpServletRequest) servletRequest;
        XssHttpServletRequestWrapper wrapper=new XssHttpServletRequestWrapper(request);
        filterChain.doFilter(wrapper,servletResponse);
    }

    @Override
    public void destroy() {

    }
}
```

## 创建分页数据封装类

在本项目中有很多模块都用到了数据分页显示，为了把分页用到的数据封装存储起来，我们需要创建一个封装类。在 `com.example.emos.api.common.util` 包里面创建 `PageUtils.java` 类。

```java
package com.example.emos.api.common.util;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PageUtils implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 总记录数
     */
    private long totalCount;
    /**
     * 每页记录数
     */
    private int pageSize;
    /**
     * 总页数
     */
    private int totalPage;
    /**
     * 当前页数
     */
    private int pageIndex;
    /**
     * 列表数据
     */
    private List list;

    public PageUtils(List list, long totalCount, int pageIndex, int pageSize) {
        this.list = list;
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.pageIndex = pageIndex;
        this.totalPage = (int) Math.ceil((double) totalCount / pageSize);
    }

}
```

## SpringDoc 页面

+ http://127.0.0.1:8090/emos-api/swagger-ui.html
