package com.nalssilog.member.application;

/**
 * 로그인한 조회자와 공개 프로필 대상 회원 사이의 노출 정책.
 * 차단 데이터의 소유 모듈이 구현해 member 모듈이 차단 저장소에 직접 의존하지 않게 한다.
 */
@FunctionalInterface
public interface MemberVisibilityPolicy {

    boolean canView(Long viewerMemberId, Long targetMemberId);
}
