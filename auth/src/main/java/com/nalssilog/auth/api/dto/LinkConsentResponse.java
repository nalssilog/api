package com.nalssilog.auth.api.dto;

/**
 * 연동 동의 응답. 프론트는 이 URL 로 브라우저를 이동시키기만 한다(기존 provider 재인증 시작).
 */
public record LinkConsentResponse(String authorizationUrl) {
}
