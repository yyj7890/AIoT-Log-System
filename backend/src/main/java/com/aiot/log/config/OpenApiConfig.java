package com.aiot.log.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiotLogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AIoT 设备运行日志管理 API")
                        .description("设备、运行日志、遥测上报、告警规则、标签、MQTT 状态和系统状态接口。"
                                + "当前文档仅用于可信网络中的开发与运维调试。")
                        .version("1.0")
                        .contact(new Contact()
                                .name("AIoT-Log-System")
                                .url("https://github.com/yyj7890/AIoT-Log-System"))
                        .license(new License()
                                .name("Project repository")
                                .url("https://github.com/yyj7890/AIoT-Log-System")));
    }

    @Bean
    public GroupedOpenApi aiotApiGroup() {
        return GroupedOpenApi.builder()
                .group("aiot-api")
                .pathsToMatch("/api/**")
                .build();
    }
}
