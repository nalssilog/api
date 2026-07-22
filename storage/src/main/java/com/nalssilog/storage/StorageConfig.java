package com.nalssilog.storage;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Cloudflare R2(S3 호환) presigner 빈. region 은 "auto", path-style 접근을 쓴다.
 * 빈 생성 시점엔 네트워크 연결이 없으므로 키가 placeholder 여도 기동은 되고, 실제 presign 시에만 유효 키가 필요.
 * report·member 가 이 단일 presigner 를 공유한다(빈 충돌 방지).
 */
@Configuration
public class StorageConfig {

    @Bean
    public S3Presigner s3Presigner(StorageProperties properties) {
        StorageProperties.R2 r2 = properties.r2();

        return S3Presigner.builder()
                .endpointOverride(URI.create(r2.endpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.accessKey(), r2.secretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
