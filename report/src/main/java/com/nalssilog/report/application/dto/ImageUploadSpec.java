package com.nalssilog.report.application.dto;

/**
 * presigned 업로드 발급을 위한 이미지 사양(내부 계약). contentType 과 파일 size(바이트).
 */
public record ImageUploadSpec(String contentType, long size) {
}
