package com.nalssilog.report.application;

import com.nalssilog.report.application.dto.ImageUploadSpec;
import com.nalssilog.report.application.dto.PresignedUpload;
import com.nalssilog.report.client.ImageStorageClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 제보 이미지 presigned 업로드 URL 발급 유스케이스. (업로드 자체는 프론트가 R2 로 직접)
 */
@Service
@RequiredArgsConstructor
public class ReportImageService {

    private final ImageStorageClient imageStorageClient;

    public List<PresignedUpload> presign(List<ImageUploadSpec> images) {
        imageStorageClient.validateImageCount(images.size());

        return images.stream()
                .map(image -> imageStorageClient.presignUpload(image.contentType(), image.size()))
                .toList();
    }
}
