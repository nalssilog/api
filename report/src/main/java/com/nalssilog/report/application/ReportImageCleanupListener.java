package com.nalssilog.report.application;

import com.nalssilog.report.client.ImageStorageClient;
import com.nalssilog.report.domain.event.ReportDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * DB 삭제가 확정된 뒤 R2 이미지를 지운다. 외부 스토리지 장애가 DB 삭제를 되돌릴 수 없으므로
 * 실패한 key 는 로그로 남기고 나머지 이미지 정리를 계속한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportImageCleanupListener {

    private final ImageStorageClient imageStorageClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void cleanup(ReportDeletedEvent event) {
        for (String imageKey : event.imageKeys()) {
            try {
                imageStorageClient.delete(imageKey);
            } catch (RuntimeException e) {
                log.error("Failed to delete report image from object storage (key={})", imageKey, e);
            }
        }
    }
}
