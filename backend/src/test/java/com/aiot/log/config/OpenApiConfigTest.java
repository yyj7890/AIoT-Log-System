package com.aiot.log.config;

import com.aiot.log.controller.AlertRuleController;
import com.aiot.log.controller.DashboardController;
import com.aiot.log.controller.DeviceController;
import com.aiot.log.controller.DeviceReportController;
import com.aiot.log.controller.EnumController;
import com.aiot.log.controller.LogController;
import com.aiot.log.controller.MqttController;
import com.aiot.log.controller.SystemController;
import com.aiot.log.controller.TagController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    private static final List<Class<?>> DOCUMENTED_CONTROLLERS = List.of(
            AlertRuleController.class,
            DashboardController.class,
            DeviceController.class,
            DeviceReportController.class,
            EnumController.class,
            LogController.class,
            MqttController.class,
            SystemController.class,
            TagController.class
    );

    @Test
    void shouldExposeProjectMetadataAndApiGroup() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.aiotLogOpenApi();
        GroupedOpenApi groupedOpenApi = config.aiotApiGroup();

        assertNotNull(openAPI.getInfo());
        assertEquals("AIoT 设备运行日志管理 API", openAPI.getInfo().getTitle());
        assertEquals("1.0", openAPI.getInfo().getVersion());
        assertNotNull(groupedOpenApi);
    }

    @Test
    void shouldDocumentEveryBusinessControllerOperation() {
        for (Class<?> controller : DOCUMENTED_CONTROLLERS) {
            assertNotNull(controller.getAnnotation(Tag.class), controller.getSimpleName() + " 缺少 @Tag");
            for (Method method : controller.getDeclaredMethods()) {
                if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    assertTrue(method.isAnnotationPresent(Operation.class),
                            controller.getSimpleName() + "." + method.getName() + " 缺少 @Operation");
                }
            }
        }
    }
}
