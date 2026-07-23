package com.nalssilog.report.application;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nalssilog.report.client.ImageStorageClient;
import com.nalssilog.report.domain.event.ReportDeletedEvent;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportImageCleanupListenerTest {

    @Test
    void continuesCleanupWhenDeletingOneImageFails() {
        ImageStorageClient imageStorageClient = mock(ImageStorageClient.class);
        ReportImageCleanupListener listener = new ReportImageCleanupListener(imageStorageClient);
        doThrow(new IllegalStateException("R2 unavailable"))
                .when(imageStorageClient).delete("reports/one.jpg");

        listener.cleanup(new ReportDeletedEvent(
                List.of("reports/one.jpg", "reports/two.jpg")));

        verify(imageStorageClient).delete("reports/one.jpg");
        verify(imageStorageClient).delete("reports/two.jpg");
    }
}
