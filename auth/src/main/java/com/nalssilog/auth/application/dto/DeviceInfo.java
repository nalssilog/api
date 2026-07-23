package com.nalssilog.auth.application.dto;

/**
 * 세션이 생성된 기기 정보. User-Agent 에서 파생한 표시용 이름 + 클라이언트 IP.
 * (로그인/갱신 시점에 캡처해 세션 메타데이터로 저장 — "로그인된 기기" 목록에 쓰인다)
 */
public record DeviceInfo(String deviceName, String ip) {
}
