package com.nalssilog.auth.web;

import com.nalssilog.auth.core.AuthService.MeState;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.Provider;
import java.util.List;

/**
 * GET /api/auth/me 응답. 프론트가 HttpOnly 쿠키를 못 읽으니 백엔드가 인증 상태를 알려주는 조회 결과.
 * (서버 세션이 아니라 AT 쿠키/티켓 쿠키를 읽어 만든 stateless 상태) 토큰·민감정보는 담지 않는다.
 */
public record MeResponse(
        boolean authenticated,
        AuthResult result,
        User user,
        PendingAuth pendingAuth
) {

    public record User(String id, String nickname, Avatar avatar) {
    }

    public record Avatar(AvatarType type, String value) {
    }

    /**
     * provider 는 이번에 시도한 소셜, existingProviders 는 기존 가입 소셜(LINK_REQUIRED 재인증 안내용).
     * SIGNUP_REQUIRED 에서는 existingProviders 가 빈 목록.
     */
    public record PendingAuth(Provider provider, String email, List<Provider> existingProviders) {
    }

    public static MeResponse authenticated(MemberInfo member) {
        User user = new User(String.valueOf(member.id()), member.nickname(),
                new Avatar(member.avatarType(), member.avatarValue()));

        return new MeResponse(true, AuthResult.SUCCESS, user, null);
    }

    public static MeResponse signupRequired(Provider provider, String email) {

        return new MeResponse(false, AuthResult.SIGNUP_REQUIRED, null, new PendingAuth(provider, email, List.of()));
    }

    public static MeResponse linkRequired(Provider provider, String email, List<Provider> existingProviders) {

        return new MeResponse(false, AuthResult.LINK_REQUIRED, null,
                new PendingAuth(provider, email, existingProviders));
    }

    public static MeResponse none() {

        return new MeResponse(false, AuthResult.NONE, null, null);
    }

    public static MeResponse from(MeState state) {

        return switch (state.status()) {
            case AUTHENTICATED -> authenticated(state.member());
            case SIGNUP_REQUIRED -> signupRequired(state.provider(), state.email());
            case LINK_REQUIRED -> linkRequired(
                    state.provider(), state.email(), state.existingProviders());
            case NONE -> none();
        };
    }
}
