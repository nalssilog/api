package com.nalssilog.report.domain;

/**
 * 제보 작성자·감사해요 주체 구분. 회원은 memberId, 익명은 HttpOnly UUID 쿠키로 식별한다.
 */
public enum ActorType {
    MEMBER,
    ANONYMOUS
}
