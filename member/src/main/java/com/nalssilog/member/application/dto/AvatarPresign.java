package com.nalssilog.member.application.dto;

/**
 * 커스텀 아바타 presigned 업로드 발급 결과(내부 계약). contentType·size 는 요청값 에코백.
 */
public record AvatarPresign(String storageKey, String uploadUrl, String contentType, long size) {
}
