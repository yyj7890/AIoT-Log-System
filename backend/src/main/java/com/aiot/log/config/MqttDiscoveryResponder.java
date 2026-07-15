package com.aiot.log.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;

/**
 * Replies to ESP32 LAN discovery requests with the current private MQTT broker address.
 * This service is intentionally independent from the MQTT report subscriber.
 */
@Component
public class MqttDiscoveryResponder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MqttDiscoveryResponder.class);
    private static final String PROTOCOL = "aiot-mqtt-discovery-v1";
    private static final int MAX_PACKET_SIZE = 2048;

    private final MqttDiscoveryProperties properties;
    private final MqttProperties mqttProperties;
    private final ObjectMapper objectMapper;
    private volatile boolean running;
    private DatagramSocket socket;
    private Thread worker;

    public MqttDiscoveryResponder(MqttDiscoveryProperties properties, MqttProperties mqttProperties,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.mqttProperties = mqttProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (mqttProperties.isRemoteMode()) {
            log.info("MQTT discovery responder is disabled in remote MQTT mode.");
            return;
        }
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            log.info("MQTT discovery responder is disabled.");
            return;
        }

        int port = properties.getPort() == null ? 19830 : properties.getPort();
        if (port < 1 || port > 65535) {
            log.error("MQTT discovery responder is disabled because UDP port {} is invalid.", port);
            return;
        }

        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            socket.setSoTimeout(1000);
            running = true;
            worker = new Thread(this::receiveLoop, "mqtt-discovery-responder");
            worker.setDaemon(true);
            worker.start();
            log.info("MQTT discovery responder is listening on UDP {}.", port);
        } catch (IOException exception) {
            closeSocket();
            log.error("MQTT discovery responder could not bind UDP {}.", port, exception);
        }
    }

    private void receiveLoop() {
        while (running) {
            DatagramPacket request = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
            try {
                socket.receive(request);
                handleRequest(request);
            } catch (java.net.SocketTimeoutException ignored) {
                // Allows the loop to observe shutdown promptly.
            } catch (SocketException exception) {
                if (running) {
                    log.warn("MQTT discovery responder socket failed.", exception);
                }
                return;
            } catch (IOException exception) {
                log.warn("MQTT discovery responder failed to receive a request.", exception);
            }
        }
    }

    private void handleRequest(DatagramPacket request) {
        String source = request.getAddress().getHostAddress() + ":" + request.getPort();
        try {
            String payload = new String(request.getData(), request.getOffset(), request.getLength(), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(payload);
            String protocol = text(root, "protocol");
            String nonce = text(root, "nonce");
            String deviceCode = text(root, "deviceCode");

            if (!PROTOCOL.equals(protocol)) {
                reject(source, deviceCode, "unsupported protocol");
                return;
            }
            if (!StringUtils.hasText(nonce)) {
                reject(source, deviceCode, "missing nonce");
                return;
            }
            if (!isTokenAccepted(text(root, "token"))) {
                reject(source, deviceCode, "token mismatch");
                return;
            }

            String brokerHost = resolveBrokerHost(request.getAddress());
            if (brokerHost == null) {
                reject(source, deviceCode, "no matching private broker address");
                return;
            }

            int brokerPort = properties.getBrokerPort() == null ? 1883 : properties.getBrokerPort();
            if (brokerPort < 1 || brokerPort > 65535) {
                reject(source, deviceCode, "invalid broker port");
                return;
            }

            var response = objectMapper.createObjectNode();
            response.put("protocol", PROTOCOL);
            response.put("nonce", nonce);
            response.put("host", brokerHost);
            response.put("port", brokerPort);
            response.put("tls", Boolean.TRUE.equals(properties.getTls()));
            response.put("token", properties.getToken() == null ? "" : properties.getToken());

            byte[] responseBytes = objectMapper.writeValueAsBytes(response);
            DatagramPacket reply = new DatagramPacket(responseBytes, responseBytes.length,
                    request.getAddress(), request.getPort());
            socket.send(reply);
            log.info("MQTT discovery response sent. deviceCode={}, source={}, broker={}:{}",
                    StringUtils.hasText(deviceCode) ? deviceCode : "-", source, brokerHost, brokerPort);
        } catch (Exception exception) {
            log.warn("MQTT discovery response failed. source={}", source, exception);
        }
    }

    private void reject(String source, String deviceCode, String reason) {
        log.warn("MQTT discovery request rejected. deviceCode={}, source={}, reason={}",
                StringUtils.hasText(deviceCode) ? deviceCode : "-", source, reason);
    }

    private boolean isTokenAccepted(String requestToken) {
        String expectedToken = properties.getToken();
        if (!StringUtils.hasText(expectedToken)) {
            return true;
        }
        return MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8),
                (requestToken == null ? "" : requestToken).getBytes(StandardCharsets.UTF_8));
    }

    private String resolveBrokerHost(InetAddress requestAddress) throws IOException {
        if (StringUtils.hasText(properties.getBrokerHost())) {
            InetAddress configuredAddress = InetAddress.getByName(properties.getBrokerHost());
            if (configuredAddress instanceof Inet4Address ipv4 && isPrivateIpv4(ipv4)) {
                return ipv4.getHostAddress();
            }
            log.error("Configured MQTT discovery broker host is not a private IPv4 address.");
            return null;
        }

        if (!(requestAddress instanceof Inet4Address requestIpv4) || !isPrivateIpv4(requestIpv4)) {
            return null;
        }

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                continue;
            }
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                if (interfaceAddress.getAddress() instanceof Inet4Address localIpv4
                        && isPrivateIpv4(localIpv4)
                        && isSameSubnet(localIpv4, requestIpv4, interfaceAddress.getNetworkPrefixLength())) {
                    return localIpv4.getHostAddress();
                }
            }
        }
        return null;
    }

    private boolean isSameSubnet(Inet4Address left, Inet4Address right, short prefixLength) {
        if (prefixLength < 1 || prefixLength > 32) {
            return false;
        }
        int mask = prefixLength == 32 ? -1 : -1 << (32 - prefixLength);
        return (toInt(left) & mask) == (toInt(right) & mask);
    }

    private int toInt(Inet4Address address) {
        byte[] bytes = address.getAddress();
        return ((bytes[0] & 0xff) << 24)
                | ((bytes[1] & 0xff) << 16)
                | ((bytes[2] & 0xff) << 8)
                | (bytes[3] & 0xff);
    }

    private boolean isPrivateIpv4(Inet4Address address) {
        byte[] bytes = address.getAddress();
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        return first == 10
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168);
    }

    private String text(JsonNode root, String fieldName) {
        JsonNode field = root == null ? null : root.get(fieldName);
        return field != null && field.isTextual() ? field.asText() : "";
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        closeSocket();
        if (worker != null) {
            worker.interrupt();
        }
    }

    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
