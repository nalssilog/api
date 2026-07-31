package com.nalssilog.auth.web;

/**
 * 세션 조회로 표현되는 안정 상태. (콜백 전용 전이 상태 LINK_SUCCESS/LINK_FAILED/FAILED 는 여기 없음)
 */
public enum AuthResult {
    SUCCESS,
    SIGNUP_REQUIRED,
    LINK_REQUIRED,
    NONE
}
