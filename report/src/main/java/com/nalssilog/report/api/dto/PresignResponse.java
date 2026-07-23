package com.nalssilog.report.api.dto;

import com.nalssilog.report.application.dto.PresignedUpload;
import java.util.List;

/**
 * presigned 업로드 발급 응답. 프론트는 각 uploadUrl 로 파일을 PUT(Content-Type 헤더 동일)한 뒤,
 * 제보 작성 요청의 imageKeys 에 storageKey 들을 담아 보낸다.
 * <b>uploads 순서는 요청 images 배열 순서와 정확히 1:1 일치한다.</b> contentType·size 도 에코백해
 * 프론트가 인덱스 없이도 어떤 파일의 URL 인지 대응할 수 있다.
 */
public record PresignResponse(List<PresignedImage> uploads) {

    public record PresignedImage(String storageKey, String uploadUrl, String contentType, long size) {
    }

    public static PresignResponse from(List<PresignedUpload> uploads) {
        List<PresignedImage> items = uploads.stream()
                .map(upload -> new PresignedImage(
                        upload.storageKey(), upload.uploadUrl(), upload.contentType(), upload.size()))
                .toList();

        return new PresignResponse(items);
    }
}
