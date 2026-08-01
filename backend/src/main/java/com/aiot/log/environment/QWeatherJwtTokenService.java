package com.aiot.log.environment;

import com.aiot.log.config.EnvironmentProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class QWeatherJwtTokenService {
    private final EnvironmentProperties properties;
    public QWeatherJwtTokenService(EnvironmentProperties properties) { this.properties = properties; }
    public String createToken() {
        try {
            long now = Instant.now().getEpochSecond();
            String header = json("{\"alg\":\"EdDSA\",\"kid\":\"" + properties.getJwtKid() + "\"}");
            String payload = json("{\"sub\":\"" + properties.getJwtProjectId() + "\",\"iat\":" + now + ",\"exp\":" + (now + 300) + "}");
            String unsigned = header + "." + payload;
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(loadPrivateKey()); signature.update(unsigned.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return unsigned + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
        } catch (Exception exception) { throw new IllegalStateException("qweather_jwt_generation_failed", exception); }
    }
    private PrivateKey loadPrivateKey() throws Exception {
        String pem = Files.readString(Path.of(properties.getJwtPrivateKeyFile()));
        String encoded = pem.replaceAll("-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\\s", "");
        return KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
    }
    private String json(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
}
