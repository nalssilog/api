package com.nalssilog.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.api.dto.AdminModerateReportRequest;
import com.nalssilog.report.api.dto.AdminRestrictAuthorRequest;
import com.nalssilog.report.client.ImageStorageClient;
import com.nalssilog.report.domain.ModerationAction;
import com.nalssilog.report.domain.ModerationStatus;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.report.domain.ReportModerationCommand;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import com.nalssilog.report.domain.WeatherReport;
import com.nalssilog.report.repository.ActorRestrictionJpaRepository;
import com.nalssilog.report.repository.ModerationActionJpaRepository;
import com.nalssilog.report.repository.ReportFlagJpaRepository;
import com.nalssilog.report.repository.WeatherReportRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960")
class AdminModerationServiceTest {

    private final WeatherReportRepository reportRepository = mock(WeatherReportRepository.class);
    private final ReportFlagJpaRepository flagRepository = mock(ReportFlagJpaRepository.class);
    private final ActorRestrictionJpaRepository restrictionRepository = mock(ActorRestrictionJpaRepository.class);
    private final ModerationActionJpaRepository actionRepository = mock(ModerationActionJpaRepository.class);
    private final ActorRestrictionService restrictionService = mock(ActorRestrictionService.class);
    private final AdminModerationService service = new AdminModerationService(
            reportRepository,
            flagRepository,
            restrictionRepository,
            actionRepository,
            restrictionService,
            mock(ImageStorageClient.class));

    @BeforeEach
    void setUp() {
        when(flagRepository.findAllByReport_IdAndStatus(any(), any())).thenReturn(List.of());
    }

    @Test
    void hideChangesPublicStateAndWritesAuditAction() {
        WeatherReport report = report();
        when(reportRepository.getReportEntity(1L)).thenReturn(report);

        var response = service.moderate(
                1L,
                99L,
                new AdminModerateReportRequest(ReportModerationCommand.HIDE, "운영 정책 위반"));

        assertThat(response.status()).isEqualTo(ModerationStatus.HIDDEN);
        assertThat(report.getModerationStatus()).isEqualTo(ModerationStatus.HIDDEN);
        verify(actionRepository).save(any(ModerationAction.class));
    }

    @Test
    void restrictionExpiryMustBeInFuture() {
        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.restrictAuthor(
                        1L,
                        99L,
                        new AdminRestrictAuthorRequest("도배", Instant.now().minusSeconds(1))));

        assertThat(exception.getErrorCode()).isEqualTo(ReportErrorCode.INVALID_RESTRICTION_EXPIRY);
    }

    private WeatherReport report() {
        return WeatherReport.ofAnonymous(
                1L, "guest", Temperature.FRESH, Precipitation.NONE, Sunlight.MODERATE, "맑아요");
    }
}
