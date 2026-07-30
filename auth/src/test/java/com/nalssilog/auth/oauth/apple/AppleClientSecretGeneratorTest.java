package com.nalssilog.auth.oauth.apple;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AppleClientSecretGeneratorTest {

    @Test
    void generatesAndCachesAppleEs256ClientSecret() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");

        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));

        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        String privateKey = """
                -----BEGIN PRIVATE KEY-----
                %s
                -----END PRIVATE KEY-----
                """.formatted(Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(keyPair.getPrivate().getEncoded()));
        AppleOAuthProperties properties = new AppleOAuthProperties(
                "TEAM123456",
                "KEY1234567",
                null,
                Base64.getEncoder().encodeToString(
                        privateKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                Duration.ofDays(30),
                Duration.ofDays(1));
        AppleClientSecretGenerator generator =
                new AppleClientSecretGenerator(properties);

        String first = generator.generate("com.nalssilog.login");
        String second = generator.generate("com.nalssilog.login");
        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(first);

        assertThat(second).isEqualTo(first);
        assertThat(parsed.getHeader().getKeyId()).isEqualTo("KEY1234567");
        assertThat(parsed.getPayload().getIssuer()).isEqualTo("TEAM123456");
        assertThat(parsed.getPayload().getSubject()).isEqualTo("com.nalssilog.login");
        assertThat(parsed.getPayload().getAudience())
                .containsExactly("https://appleid.apple.com");
        assertThat(parsed.getPayload().getExpiration().toInstant())
                .isAfter(Instant.now().plus(Duration.ofDays(29)));
    }
}
