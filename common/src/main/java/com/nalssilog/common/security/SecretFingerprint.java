package com.nalssilog.common.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class SecretFingerprint {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private SecretFingerprint() {
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    public static String hmacSha256(String secret, String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));

            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("HmacSHA256 not available", exception);
        } catch (InvalidKeyException exception) {
            throw new IllegalStateException("HmacSHA256 key is invalid", exception);
        }
    }
}
