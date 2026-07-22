package com.nalssilog.auth.client;

import com.nalssilog.member.domain.Provider;
import java.util.Map;

/**
 * 각 소셜 프로바이더의 user-info 응답을 공통 형태로 정규화한다.
 */
public record OAuthUserInfo(
        Provider provider,
        String providerUserId,
        String email,
        String nickname
) {

    private static final String EMAIL_KEY = "email";

    @SuppressWarnings("unchecked")
    public static OAuthUserInfo of(String registrationId, Map<String, Object> attributes) {
        Provider provider = Provider.from(registrationId);

        return switch (provider) {
            case GOOGLE -> new OAuthUserInfo(
                    provider,
                    String.valueOf(attributes.get("sub")),
                    (String) attributes.get(EMAIL_KEY),
                    (String) attributes.get("name")
            );
            case KAKAO -> {
                Map<String, Object> account = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
                Map<String, Object> profile = (Map<String, Object>) account.getOrDefault("profile", Map.of());

                yield new OAuthUserInfo(
                        provider,
                        String.valueOf(attributes.get("id")),
                        (String) account.get(EMAIL_KEY),
                        (String) profile.get("nickname")
                );
            }
            case NAVER -> {
                Map<String, Object> response = (Map<String, Object>) attributes.getOrDefault("response", Map.of());

                yield new OAuthUserInfo(
                        provider,
                        String.valueOf(response.get("id")),
                        (String) response.get(EMAIL_KEY),
                        (String) response.get("nickname")
                );
            }
        };
    }
}
