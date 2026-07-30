package com.nalssilog.member.domain;

import java.util.Locale;

public enum Provider {
    GOOGLE, KAKAO, NAVER, APPLE;

    public static Provider from(String registrationId) {

        return valueOf(registrationId.toUpperCase(Locale.ROOT));
    }
}
