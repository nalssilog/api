package com.nalssilog.auth.oauth;

import com.nalssilog.member.domain.Provider;
import java.util.Map;

/**
 * 각 소셜 프로바이더의 user-info 응답을 공통 형태로 정규화한다.
 */
public record OAuthUserInfo(
        Provider provider,
        String providerUserId,
        String email,
        String socialName
) {

    private static final String EMAIL_KEY = "email";

    @SuppressWarnings("unchecked")
    public static OAuthUserInfo of(String registrationId, Map<String, Object> attributes) {
        return of(registrationId, attributes, null);
    }

    @SuppressWarnings("unchecked")
    public static OAuthUserInfo of(
            String registrationId,
            Map<String, Object> attributes,
            String appleSocialName
    ) {
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
                String name = text(response.get("name"));

                yield new OAuthUserInfo(
                        provider,
                        String.valueOf(response.get("id")),
                        text(response.get(EMAIL_KEY)),
                        name == null || name.isBlank() ? text(response.get("nickname")) : name
                );
            }
            case APPLE -> new OAuthUserInfo(
                    provider,
                    text(attributes.get("sub")),
                    text(attributes.get(EMAIL_KEY)),
                    firstNonBlank(appleSocialName, text(attributes.get("name")))
            );
        };
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
