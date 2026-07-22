package com.nalssilog.report.application.dto;

import com.nalssilog.member.domain.AvatarType;

/**
 * 제보 작성자 표시 정보(활성 회원 한정). 회원이 탈퇴했거나 익명이면 이 값 없이 "익명의 이웃"으로 표시한다.
 * avatarValue 는 PRESET 이면 프리셋 id, CUSTOM 이면 이미지 URL, DEFAULT 면 null.
 */
public record AuthorInfo(Long id, String nickname, AvatarType avatarType, String avatarValue) {
}
