package com.example.emos.wx.config;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

@Configuration   // 让 SpringBoot 读取到配置信息
@EnableSwagger2  // 让 Swagger 生效
public class SwaggerConfig {
    @Bean
    public Docket createRestApi() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2);
        // ApiInfoBuilder 用于在 Swagger 界面上添加各种信息
        ApiInfoBuilder builder = new ApiInfoBuilder();
        builder.title("EMOS 在线办公系统");
        ApiInfo info = builder.build();
        docket.apiInfo(info);

        // ApiSelectorBuilder 用来设置哪些类中的方法会生成到 REST API 中
        ApiSelectorBuilder selectorBuilder = docket.select();
        // 添加所有包下的类
        selectorBuilder.paths(PathSelectors.any());
        // 使用 @ApiOperation 注解的方法才会被提取到 REST API 中
        selectorBuilder.apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class));
        docket = selectorBuilder.build();

        /*
         * 下面的语句是开启对 JWT 的支持，当用户用 Swagger 调用受 JWT 认证保护的方法时
         * 必须要先提交参数（例如 token）
         */
        ApiKey apiKey = new ApiKey("token", "token", "header");
        // 存储用户必须提交的参数
        List<ApiKey> apiKeyList = new ArrayList<>();
        // 规定用户需要输入什么参数
        apiKeyList.add(apiKey);
        docket.securitySchemes(apiKeyList);

        // 如果用户 JWT 认证通过，则在 Swagger 中全局有效
        AuthorizationScope scope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] scopes = {scope};
        // 存储 token 和作用域
        SecurityReference reference = new SecurityReference("token", scopes);
        List refList = new ArrayList();
        refList.add(reference);
        SecurityContext context = SecurityContext.builder().securityReferences(refList).build();
        List cxtList = new ArrayList();
        cxtList.add(context);
        docket.securityContexts(cxtList);

        return docket;
    }
}
