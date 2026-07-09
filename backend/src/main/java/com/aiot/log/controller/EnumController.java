package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.enums.DeviceStatus;
import com.aiot.log.enums.LogLevel;
import com.aiot.log.enums.LogSource;
import com.aiot.log.enums.LogStatus;
import com.aiot.log.enums.LogType;
import com.aiot.log.vo.EnumOptionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enums")
public class EnumController {

    @GetMapping
    public ApiResponse<Map<String, List<EnumOptionVO>>> getEnums() {
        Map<String, List<EnumOptionVO>> result = new HashMap<String, List<EnumOptionVO>>();
        result.put("deviceStatus", getDeviceStatusOptions());
        result.put("logType", getLogTypeOptions());
        result.put("logLevel", getLogLevelOptions());
        result.put("logStatus", getLogStatusOptions());
        result.put("logSource", getLogSourceOptions());
        result.put("alertMetric", getAlertMetricOptions());
        result.put("alertOperator", getAlertOperatorOptions());
        return ApiResponse.success(result);
    }

    private List<EnumOptionVO> getDeviceStatusOptions() {
        return Arrays.asList(
                new EnumOptionVO("正常", DeviceStatus.NORMAL),
                new EnumOptionVO("异常", DeviceStatus.ABNORMAL),
                new EnumOptionVO("离线", DeviceStatus.OFFLINE),
                new EnumOptionVO("维护中", DeviceStatus.MAINTENANCE)
        );
    }

    private List<EnumOptionVO> getLogTypeOptions() {
        return Arrays.asList(
                new EnumOptionVO("运行日志", LogType.RUNNING),
                new EnumOptionVO("异常日志", LogType.ERROR),
                new EnumOptionVO("维护日志", LogType.MAINTENANCE),
                new EnumOptionVO("巡检日志", LogType.INSPECTION)
        );
    }

    private List<EnumOptionVO> getLogLevelOptions() {
        return Arrays.asList(
                new EnumOptionVO("普通", LogLevel.INFO),
                new EnumOptionVO("警告", LogLevel.WARNING),
                new EnumOptionVO("严重", LogLevel.ERROR)
        );
    }

    private List<EnumOptionVO> getLogStatusOptions() {
        return Arrays.asList(
                new EnumOptionVO("待处理", LogStatus.PENDING),
                new EnumOptionVO("处理中", LogStatus.PROCESSING),
                new EnumOptionVO("已解决", LogStatus.RESOLVED)
        );
    }

    private List<EnumOptionVO> getLogSourceOptions() {
        return Arrays.asList(
                new EnumOptionVO("人工录入", LogSource.MANUAL),
                new EnumOptionVO("设备上报", LogSource.DEVICE),
                new EnumOptionVO("系统生成", LogSource.SYSTEM)
        );
    }

    private List<EnumOptionVO> getAlertMetricOptions() {
        return Arrays.asList(
                new EnumOptionVO("温度", "temperature"),
                new EnumOptionVO("湿度", "humidity"),
                new EnumOptionVO("电压", "voltage"),
                new EnumOptionVO("信号强度", "signalStrength")
        );
    }

    private List<EnumOptionVO> getAlertOperatorOptions() {
        return Arrays.asList(
                new EnumOptionVO("大于", "GT"),
                new EnumOptionVO("小于", "LT"),
                new EnumOptionVO("大于等于", "GTE"),
                new EnumOptionVO("小于等于", "LTE"),
                new EnumOptionVO("等于", "EQ")
        );
    }
}
