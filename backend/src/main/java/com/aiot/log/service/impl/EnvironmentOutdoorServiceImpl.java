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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

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
            JsonNode air = request(host + "/airquality/v1/current/" + lat + "/" + lon + "?lang=zh");
            JsonNode now = weather.path("now");
            JsonNode index = air.path("indexes").isArray() && !air.path("indexes").isEmpty() ? air.path("indexes").get(0) : json.createObjectNode();
            EnvironmentOutdoorReading reading = new EnvironmentOutdoorReading();
            reading.spaceId = spaceId; reading.temperature = decimal(now, "temp"); reading.humidity = decimal(now, "humidity");
            reading.weatherText = text(now, "text"); reading.feelsLike = decimal(now, "feelsLike");
            reading.windDir = text(now, "windDir"); reading.windScale = text(now, "windScale"); reading.windSpeed = decimal(now, "windSpeed");
            reading.precip = decimal(now, "precip"); reading.pressure = decimal(now, "pressure"); reading.visibility = decimal(now, "vis");
            reading.cloud = integer(now, "cloud"); reading.dew = decimal(now, "dew");
            reading.aqi = integer(index, "aqi"); reading.aqiCategory = text(index, "category");
            reading.primaryPollutant = text(index.path("primaryPollutant"), "name");
            reading.healthAdviceGeneral = text(index.path("health").path("advice"), "generalPopulation");
            reading.healthAdviceSensitive = text(index.path("health").path("advice"), "sensitivePopulation");
            reading.pm2p5 = pollutant(air, "pm2p5"); reading.pm10 = pollutant(air, "pm10"); reading.no2 = pollutant(air, "no2");
            reading.o3 = pollutant(air, "o3"); reading.so2 = pollutant(air, "so2"); reading.co = pollutant(air, "co");
            reading.pm2p5Unit = pollutantUnit(air, "pm2p5"); reading.pm10Unit = pollutantUnit(air, "pm10"); reading.no2Unit = pollutantUnit(air, "no2");
            reading.o3Unit = pollutantUnit(air, "o3"); reading.so2Unit = pollutantUnit(air, "so2"); reading.coUnit = pollutantUnit(air, "co");
            reading.stationName = air.path("stations").isArray() && !air.path("stations").isEmpty() ? text(air.path("stations").get(0), "name") : null;
            reading.observedAt = LocalDateTime.now();
            readings.insert(reading); evaluator.evaluate(reading); return toVO(reading);
        } catch (BusinessException exception) { throw exception; }
        catch (Exception exception) { throw new BusinessException(ErrorCode.ENVIRONMENT_WEATHER_REQUEST_FAILED, diagnostic(exception), exception); }
    }

    @Override public EnvironmentOutdoorReadingVO latest(Long spaceId) {
        EnvironmentOutdoorReading reading = readings.selectOne(new LambdaQueryWrapper<EnvironmentOutdoorReading>()
                .eq(EnvironmentOutdoorReading::getSpaceId, spaceId).orderByDesc(EnvironmentOutdoorReading::getObservedAt).last("LIMIT 1"));
        return reading == null ? null : toVO(reading);
    }
    private JsonNode request(String url) throws Exception {
        HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + jwt.createToken()).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new IllegalStateException("qweather_http_" + response.statusCode());
        JsonNode body = json.readTree(responseText(response));
        if (body.has("code") && !"200".equals(body.path("code").asText())) throw new IllegalStateException("qweather_api_" + body.path("code").asText());
        return body;
    }
    private String responseText(HttpResponse<byte[]> response) throws Exception {
        byte[] raw = response.body();
        String encoding = response.headers().firstValue("Content-Encoding").orElse("").toLowerCase(Locale.ROOT);
        boolean gzip = encoding.contains("gzip") || (raw.length >= 2 && raw[0] == (byte) 0x1f && raw[1] == (byte) 0x8b);
        if (!gzip) return new String(raw, StandardCharsets.UTF_8);
        try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(raw))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
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
    private BigDecimal pollutant(JsonNode air, String code) { for (JsonNode item : air.path("pollutants")) if (code.equals(item.path("code").asText())) return decimal(item.path("concentration"), "value"); return null; }
    private String pollutantUnit(JsonNode air, String code) { for (JsonNode item : air.path("pollutants")) if (code.equals(item.path("code").asText())) return text(item.path("concentration"), "unit"); return null; }
    private EnvironmentOutdoorReadingVO toVO(EnvironmentOutdoorReading r) { EnvironmentOutdoorReadingVO v = new EnvironmentOutdoorReadingVO(); v.temperature=r.temperature; v.humidity=r.humidity; v.weatherText=r.weatherText; v.feelsLike=r.feelsLike; v.windDir=r.windDir; v.windScale=r.windScale; v.windSpeed=r.windSpeed; v.precip=r.precip; v.pressure=r.pressure; v.visibility=r.visibility; v.cloud=r.cloud; v.dew=r.dew; v.aqi=r.aqi; v.aqiCategory=r.aqiCategory; v.pm2p5=r.pm2p5; v.pm2p5Unit=r.pm2p5Unit; v.pm10=r.pm10; v.pm10Unit=r.pm10Unit; v.no2=r.no2; v.no2Unit=r.no2Unit; v.o3=r.o3; v.o3Unit=r.o3Unit; v.so2=r.so2; v.so2Unit=r.so2Unit; v.co=r.co; v.coUnit=r.coUnit; v.primaryPollutant=r.primaryPollutant; v.healthAdviceGeneral=r.healthAdviceGeneral; v.healthAdviceSensitive=r.healthAdviceSensitive; v.stationName=r.stationName; v.observedAt=r.observedAt; return v; }
}
