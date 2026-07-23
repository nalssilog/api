package com.nalssilog.report.domain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 익명 식별자를 노출하지 않고 화면용 익명 닉네임을 안정적으로 생성한다.
 * 같은 익명 식별자는 항상 같은 10자리 숫자를 얻는다.
 */
public final class AnonymousNicknameGenerator {

    private static final long NUMBER_BOUND = 10_000_000_000L;

    private AnonymousNicknameGenerator() {
    }

    public static String generate(String stableKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(stableKey.getBytes(StandardCharsets.UTF_8));
            long number = Long.remainderUnsigned(ByteBuffer.wrap(digest).getLong(), NUMBER_BOUND);

            return "익명#%010d".formatted(number);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
