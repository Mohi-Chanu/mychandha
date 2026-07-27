package com.mychandha.platform.security.ratelimit;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"api", "local", "test"})
final class RateLimitKeyHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKeySpec processKey;

    RateLimitKeyHasher() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        processKey = new SecretKeySpec(key, ALGORITHM);
    }

    String digest(String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(processKey);
            return HexFormat.of().formatHex(
                    mac.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Rate-limit key hashing is unavailable", exception);
        }
    }
}
