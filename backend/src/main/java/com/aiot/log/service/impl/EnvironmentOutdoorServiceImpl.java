package com.aiot.log.service.impl;

import com.aiot.log.config.EnvironmentProperties;
import com.aiot.log.entity.EnvironmentOutdoorReading;
import com.aiot.log.entity.EnvironmentSpace;
import com.aiot.log.environment.QWeatherJwtTokenService;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.mapper.EnvironmentOutdoorReadingMapper;
import com.aiot.log.mapper.EnvironmentSpaceMapper;
import com.aiot.log.service.EnvironmentAnnouncementRuleEvaluator;
import com.aiot.log.service.EnvironmentOutdoorService;
import com.aiot.log.vo.EnvironmentOutdoorReadingVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.net.URI;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

@Service
public class EnvironmentOutdoorServiceImpl implements EnvironmentOutdoorService {
    private final EnvironmentSpaceMapper spaces;
    private final EnvironmentOutdoorReadingMapper readings;
    private final EnvironmentProperties properties;
    private final QWeatherJwtTokenService jwt;
    private final ObjectMapper json;
    private final EnvironmentAnnouncementRuleEvaluator evaluator;
    private final HttpClient http = HttpClient.newHttpClient();

    public EnvironmentOutdoorServiceImpl(EnvironmentSpaceMapper spaces, EnvironmentOutdoorReadingMapper readings,
            EnvironmentProperties properties, QWeatherJwtTokenService jwt, ObjectMapper json,
            EnvironmentAnnouncementRuleEvaluator evaluator) {
        this.spaces = spaces; this.readings = readings; this.properties = properties;
        this.jwt = jwt; this.json = json; this.evaluator = evaluator;
    }

    @Override public EnvironmentOutdoorReadingVO refresh(Long spaceId) {
        EnvironmentSpace space = spaces.selectById(spaceId);
        if (space == null) throw new BusinessException(ErrorCode.ENVIRONMENT_SPACE_NOT_FOUND);
        if (!properties.isConfigured() || space.getLatitude() == null || space.getLongitude() == null)
            throw new BusinessException(ErrorCode.ENVIRONMENT_WEATHER_NOT_CONFIGURED);
        try {
            String host = properties.getApiHost().replaceAll("/$", "");
            if (!host.matches("^https?://.*")) host = "https://" + host;
            String lat = space.getLatitude().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            String lon = space.getLongitude().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            JsonNode weather = request(host + "/v7/weather/now?location=" + lon + "," + lat + "&lang=zh");
            JsonNode air = request(host + "/airquality/v1/current/" + lat + "/" + lon);
            JsonNode now = weather.path("now");
            JsonNode index = air.path("indexes").isArray() && !air.path("indexes").isEmpty() ? air.path("indexes").get(0) : json.createObjectNode();
            EnvironmentOutdoorReading reading = new EnvironmentOutdoorReading();
            reading.spaceId = spaceId; reading.temperature = decimal(now, "temp"); reading.humidity = decimal(now, "humidity");
            reading.weatherText = text(now, "text"); reading.aqi = integer(index, "aqi");
            reading.primaryPollutant = text(index.path("primaryPollutant"), "name"); reading.observedAt = LocalDateTime.now();
            readings.insert(reading); evaluator.evaluate(reading); return toVO(reading);
        } catch (BusinessException exception) { throw exception; }
        catch (Exception exception) { throw new BusinessException(ErrorCode.ENVIRONMENT_WEATHER_REQUEST_FAILED, diagnostic(exception)); }
    }

    @Override public EnvironmentOutdoorReadingVO latest(Long spaceId) {
        EnvironmentOutdoorReading reading = readings.selectOne(new LambdaQueryWrapper<EnvironmentOutdoorReading>()
                .eq(EnvironmentOutdoorReading::getSpaceId, spaceId).orderByDesc(EnvironmentOutdoorReading::getObservedAt).last("LIMIT 1"));
        return reading == null ? null : toVO(reading);
    }
    private JsonNode request(String url) throws Exception {
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + jwt.createToken()).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("qweather_http_" + response.statusCode());
        JsonNode body = json.readTree(response.body());
        if (body.has("code") && !"200".equals(body.path("code").asText())) throw new IllegalStateException("qweather_api_" + body.path("code").asText());
        return body;
    }
    private String diagnostic(Exception exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage() == null ? "" : cause.getMessage();
            if (message.contains("qweather_jwt_generation_failed")) return "天气 JWT 私钥或凭证格式无效";
            if (message.startsWith("qweather_http_")) return "天气服务 HTTP 状态：" + message.substring(14);
            if (message.startsWith("qweather_api_")) return "天气服务业务状态：" + message.substring(13);
            if (cause instanceof UnknownHostException || cause instanceof java.nio.channels.UnresolvedAddressException)
                return "天气服务 Host 无法解析";
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException)
                return "天气服务请求超时";
            if (cause instanceof ConnectException) return "天气服务连接失败";
            if (cause instanceof javax.net.ssl.SSLException || message.contains("SSL"))
                return "天气服务 TLS 连接失败";
        }
        return "天气服务请求失败，请检查后端运行日志";
    }
    private BigDecimal decimal(JsonNode node, String key) { return node.hasNonNull(key) ? new BigDecimal(node.get(key).asText()) : null; }
    private Integer integer(JsonNode node, String key) { try { return node.hasNonNull(key) ? Integer.valueOf(node.get(key).asText()) : null; } catch (NumberFormatException e) { return null; } }
    private String text(JsonNode node, String key) { return node.hasNonNull(key) ? node.get(key).asText() : null; }
    private EnvironmentOutdoorReadingVO toVO(EnvironmentOutdoorReading r) { EnvironmentOutdoorReadingVO v = new EnvironmentOutdoorReadingVO(); v.temperature=r.temperature; v.humidity=r.humidity; v.weatherText=r.weatherText; v.aqi=r.aqi; v.pm2p5=r.pm2p5; v.primaryPollutant=r.primaryPollutant; v.observedAt=r.observedAt; return v; }
}
