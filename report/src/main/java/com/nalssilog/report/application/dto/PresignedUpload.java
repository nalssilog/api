package com.nalssilog.report.application.dto;

/**
 * presigned 업로드 1건. 프론트는 uploadUrl 로 파일을 PUT 하고, 제보 작성 시 storageKey 를 imageKeys 에 담아 보낸다.
 * contentType·size 는 요청값 에코백(프론트가 어떤 파일의 URL 인지 안전하게 대응하도록).
 */
public record PresignedUpload(String storageKey, String uploadUrl, String contentType, long size) {
}
