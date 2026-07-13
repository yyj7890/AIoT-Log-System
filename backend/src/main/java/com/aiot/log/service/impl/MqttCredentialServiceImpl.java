package com.aiot.log.service.impl;

import com.aiot.log.dto.MqttGlobalCredentialUpdateRequest;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.service.MqttCredentialService;
import com.aiot.log.vo.MqttGlobalCredentialStatusVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MqttCredentialServiceImpl implements MqttCredentialService {

    private final Path configDirectory = resolveConfigDirectory();

    @Override
    public MqttGlobalCredentialStatusVO getGlobalCredentialStatus() {
        Path credentials = configDirectory.resolve("mqtt-credentials.env");
        String username = "";
        boolean passwordConfigured = false;
        try {
            if (Files.exists(credentials)) {
                for (String line : Files.readAllLines(credentials, StandardCharsets.UTF_8)) {
                    if (line.startsWith("MQTT_USERNAME=")) username = line.substring("MQTT_USERNAME=".length());
                    if (line.startsWith("MQTT_PASSWORD=") && StringUtils.hasText(line.substring("MQTT_PASSWORD=".length()))) passwordConfigured = true;
                }
            }
        } catch (IOException exception) {
            throw new BusinessException(500, "无法读取 MQTT 凭证状态");
        }
        MqttGlobalCredentialStatusVO vo = new MqttGlobalCredentialStatusVO();
        vo.setUsername(username);
        vo.setPasswordConfigured(passwordConfigured);
        vo.setAnonymousAccessEnabled(isAnonymousAccessEnabled());
        vo.setActivationPending(passwordConfigured && isAnonymousAccessEnabled());
        return vo;
    }

    @Override
    public synchronized MqttGlobalCredentialStatusVO saveGlobalCredential(MqttGlobalCredentialUpdateRequest request) {
        try {
            Files.createDirectories(configDirectory);
            runMosquittoPasswordTool(request.getUsername(), request.getPassword());
            Files.writeString(configDirectory.resolve("mosquitto-acl.conf"),
                    "# Shared global account for locally configured IoT devices.\n"
                            + "user " + request.getUsername() + "\n"
                            + "topic readwrite aiot/device/+/report\n"
                            + "topic readwrite aiot/device/+/log\n", StandardCharsets.UTF_8);
            Files.writeString(configDirectory.resolve("mqtt-credentials.env"),
                    "# Local global MQTT credential. Do not commit or share this file.\n"
                            + "MQTT_USERNAME=" + request.getUsername() + "\n"
                            + "MQTT_PASSWORD=" + request.getPassword() + "\n", StandardCharsets.UTF_8);
            return getGlobalCredentialStatus();
        } catch (IOException exception) {
            throw new BusinessException(500, "保存 MQTT 全局凭证失败：" + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "保存 MQTT 全局凭证被中断");
        }
    }

    @Override
    public synchronized MqttGlobalCredentialStatusVO setAuthenticationEnabled(boolean enabled) {
        MqttGlobalCredentialStatusVO status = getGlobalCredentialStatus();
        if (enabled && !Boolean.TRUE.equals(status.getPasswordConfigured())) {
            throw new BusinessException(400, "请先保存全局 MQTT 用户名和密码");
        }
        Path config = configDirectory.resolve("mosquitto-lan.conf");
        try {
            List<String> lines = Files.readAllLines(config, StandardCharsets.UTF_8);
            String replacement = "allow_anonymous " + (!enabled);
            boolean replaced = false;
            for (int index = 0; index < lines.size(); index++) {
                if (lines.get(index).trim().startsWith("allow_anonymous")) {
                    lines.set(index, replacement);
                    replaced = true;
                }
            }
            if (!replaced) lines.add(replacement);
            Files.writeString(config, lines.stream().collect(Collectors.joining(System.lineSeparator()))
                    + System.lineSeparator(), StandardCharsets.UTF_8);
            return getGlobalCredentialStatus();
        } catch (IOException exception) {
            throw new BusinessException(500, "更新 MQTT 认证开关失败");
        }
    }

    private void runMosquittoPasswordTool(String username, String password) throws IOException, InterruptedException {
        String executable = System.getenv().getOrDefault("MOSQUITTO_PASSWD_EXECUTABLE", "C:\\Program Files\\mosquitto\\mosquitto_passwd.exe");
        Process process = new ProcessBuilder(executable, "-b", "-c",
                configDirectory.resolve("mosquitto-passwords").toString(), username, password)
                .redirectErrorStream(true).start();
        if (process.waitFor() != 0) {
            throw new IOException("Mosquitto 密码工具执行失败");
        }
    }

    private boolean isAnonymousAccessEnabled() {
        try {
            Path config = configDirectory.resolve("mosquitto-lan.conf");
            return !Files.exists(config) || Files.readAllLines(config, StandardCharsets.UTF_8).stream()
                    .noneMatch(line -> line.trim().equalsIgnoreCase("allow_anonymous false"));
        } catch (IOException exception) {
            return true;
        }
    }

    private Path resolveConfigDirectory() {
        String configured = System.getenv("IOT_CONFIG_DIR");
        if (StringUtils.hasText(configured)) return Path.of(configured);
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        return current.getFileName().toString().equals("backend") ? current.getParent().resolve("config") : current.resolve("config");
    }
}
