package com.nalssilog.auth.application;

import com.nalssilog.auth.application.dto.SessionData;
import com.nalssilog.auth.application.dto.SessionView;
import com.nalssilog.auth.domain.AuthErrorCode;
import com.nalssilog.auth.repository.RefreshTokenStore;
import com.nalssilog.common.exception.NalssiLogException;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 로그인된 기기(세션) 관리. 목록 조회 + 특정 기기 로그아웃.
 * currentTokenHash 는 현재 요청의 refresh 쿠키에서 파생한 세션 키로, '이 기기' 표시/판별에 쓴다.
 */
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final RefreshTokenStore refreshTokenStore;

    public List<SessionView> listSessions(Long memberId, String currentTokenHash) {
        return refreshTokenStore.findSessionsByMember(memberId).stream()
                .sorted(Comparator.comparing(SessionData::lastActiveAt).reversed())
                .map(session -> new SessionView(
                        session.sessionId(),
                        session.deviceName(),
                        session.ip(),
                        session.loginAt(),
                        session.lastActiveAt(),
                        session.tokenHash().equals(currentTokenHash)))
                .toList();
    }

    /**
     * 특정 세션(기기) 로그아웃. 대상이 현재 세션이면 true 를 반환해 컨트롤러가 쿠키까지 정리하게 한다.
     */
    public boolean revokeSession(Long memberId, String sessionId, String currentTokenHash) {
        SessionData target = refreshTokenStore.findSessionsByMember(memberId).stream()
                .filter(session -> session.sessionId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.SESSION_NOT_FOUND));

        refreshTokenStore.delete(target.tokenHash());

        return target.tokenHash().equals(currentTokenHash);
    }
}
