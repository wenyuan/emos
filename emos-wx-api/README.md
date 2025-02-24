## 环境版本

+ JDK 15.0.2
+ SpringBoot 2.3.4
+ MySQL 8.0
+ [Maven 3.6.3](https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.zip)

## IDEA 插件

+ Lombok 插件：编译字节码的时候自动生成 GET/SET 方法
+ Free MyBatis Plus 插件/Free MyBatis Tool 插件：
  + 创建数据库连接
  + 根据选中的数据表生成各种 MyBatis 文件

## 项目初始化依赖库

| 序号 | 依赖库              | 功能                  |
| :--: | ------------------- | --------------------- |
|  1   | SpringBoot DevTool  | 热部署插件            |
|  2   | Lombok              | 自动生成 GET/SET 方法 |
|  3   | Spring Web          | SpringMVC 框架        |
|  4   | MySQL Driver        | MySQL 数据库驱动      |
|  5   | MyBatis Framework   | MyBatis 持久层框架    |
|  6   | Spring Data Redis   | 操作 Redis 技术       |
|  7   | Spring Data MongoDB | 操作 MongoDB技术      |
|  8   | Spring For RabbitMQ | RabbitMQ 驱动         |
|  9   | Java Mail Sender    | 邮件发送技术          |
|  10  | QuartZ Scheduler    | 定时器技术            |

## 主要功能

### 配置 SpringBoot 项目（基础功能）

| 序号 | 功能项                               | 备注                                         |
| :--: | ------------------------------------ | -------------------------------------------- |
|  1   | 利用 Maven 创建 SpringBoot 项目      | 项目初始化                                   |
|  2   | 配置 MySQL、MongoDB 和 Redis 数据源  | 基础数据库配置                               |
|  3   | 整合 SSM 框架                        | 目前主要是 MyBatis 的配置                    |
|  4   | 自定义异常类和封装结果集             | 通用功能封装                                 |
|  5   | 集成 Swagger，便于调用测试 Web 方法  | 通用功能封装（可选）                         |
|  6   | 配置后端验证功能                     | 通用功能封装                                 |
|  7   | 抵御跨站脚本攻击                     | 通用功能封装                                 |
|  8   | 整合 Shiro 和 JWT                    | 通用功能封装                                 |
|  9   | 实现令牌自动刷新                     | 通用功能封装                                 |
|  10  | 精简返回给客户端的异常内容           | 通用功能封装                                 |
|  11  | 注册（没有密码，直接微信小程序登录） | 通用功能封装（唯一的超级管理员，固定邀请码） |
|  12  | 登录（没有密码，直接微信小程序登录） | 用功能封装                                   |
|  13  | RBAC 权限模型                        | 通用功能封装                                 |

### 业务功能模块（定制功能）

| 序号 | 功能项                             | 备注                                                         |
| :--: | ---------------------------------- | ------------------------------------------------------------ |
|  1   | 签到功能，检查当前时刻是否可以签到 | 需要定义一个封装类用于缓存系统常量数据                       |
|  2   | 签到异常发送邮件                   | 封装声明 Java 线程池的配置类 -> 定义线程任务                 |
|  3   | 员工考勤记录查询                   | 业务逻辑和 SQL 查询比较复杂                                  |
|  4   | 消息通知模块（RabbitMQ + MongoDB） | 使用异步线程的方式收发消息：<br />注册后发送系统消息<br />登录后接收系统消息（从 MQ 中接收消息后，保存到 Ref 集合）<br />首页定时轮询接收消息（查询用户最新接收到的消息条目） |

### TODO...

10

## Swagger 页面

+ http://127.0.0.1:8080/emos-wx-api/swagger-ui.html
