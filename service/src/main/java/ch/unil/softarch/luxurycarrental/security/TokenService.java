package ch.unil.softarch.luxurycarrental.security;

import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and checks signed API tokens: {@code base64url(role|id|expiry).base64url(HMAC-SHA256)}.
 * <p>
 * The signing key comes from {@code API_TOKEN_SECRET}; without it a random key is created at
 * start-up, which logs everybody out when the server restarts.
 * </p>
 */
@ApplicationScoped
public class TokenService {

    private static final Duration LIFETIME = Duration.ofHours(8);
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final byte[] key;

    public TokenService() {
        String secret = System.getenv("API_TOKEN_SECRET");
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            key = random;
            System.out.println("[TokenService] API_TOKEN_SECRET not set - using a random key for this run.");
        } else {
            key = secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String issue(Role role, UUID id) {
        long expires = Instant.now().plus(LIFETIME).getEpochSecond();
        String payload = role + "|" + id + "|" + expires;
        String body = ENC.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + ENC.encodeToString(sign(body));
    }

    /** The caller described by a valid, unexpired token; empty for anything else. */
    public Optional<Caller> verify(String token) {
        try {
            int dot = token.indexOf('.');
            if (dot <= 0) {
                return Optional.empty();
            }
            String body = token.substring(0, dot);
            byte[] signature = DEC.decode(token.substring(dot + 1));
            if (!MessageDigest.isEqual(signature, sign(body))) {
                return Optional.empty();
            }
            String[] parts = new String(DEC.decode(body), StandardCharsets.UTF_8).split("\\|");
            if (parts.length != 3 || Instant.now().getEpochSecond() > Long.parseLong(parts[2])) {
                return Optional.empty();
            }
            return Optional.of(new Caller(Role.valueOf(parts[0]), UUID.fromString(parts[1])));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private byte[] sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 not available", e);
        }
    }
}
