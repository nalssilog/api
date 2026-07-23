package com.nalssilog.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 오브젝트 스토리지(Cloudflare R2, S3 호환)의 공유 설정 — R2 접속 정보 + 공개 도메인.
 * 도메인별 정책(경로 prefix·최대 크기·허용 형식)은 사용하는 모듈(report·member)이 각자 들고 있는다.
 * r2 시크릿은 application-local/prod.yml 에서 채운다.
 * verifyUpload: 업로드 후 R2 HEAD 검증(존재·크기·타입) 활성화 여부. R2/MinIO 가 연결된 dev/prod 만 true, local 은 false.
 */
@ConfigurationProperties(prefix = "nalssilog.storage")
public record StorageProperties(
        R2 r2,
        String publicBaseUrl,
        boolean verifyUpload
) {

    public record R2(String endpoint, String accessKey, String secretKey, String bucket) {
    }
}
