package com.nalssilog.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.api.dto.CreateReportFlagRequest;
import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.report.domain.ReportFlag;
import com.nalssilog.report.domain.ReportFlagReason;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import com.nalssilog.report.domain.WeatherReport;
import com.nalssilog.report.repository.ReportFlagJpaRepository;
import com.nalssilog.report.repository.WeatherReportRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("java:S5960")
class ReportFlagServiceTest {

    private final WeatherReportRepository reportRepository = mock(WeatherReportRepository.class);
    private final ReportFlagJpaRepository flagRepository = mock(ReportFlagJpaRepository.class);
    private final ReportFlagService service = new ReportFlagService(reportRepository, flagRepository);

    @Test
    void authorCannotFlagOwnReport() {
        when(reportRepository.getVisibleReportEntity(1L)).thenReturn(memberReport(7L));

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.flag(1L, ReportActor.member(7L), request()));

        assertThat(exception.getErrorCode()).isEqualTo(ReportErrorCode.CANNOT_FLAG_OWN_REPORT);
        verify(flagRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateFlagIsRejected() {
        when(reportRepository.getVisibleReportEntity(1L)).thenReturn(memberReport(7L));
        when(flagRepository.existsByReport_IdAndReporterTypeAndReporterKey(
                1L, ReportActor.member(8L).type(), "8")).thenReturn(true);

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.flag(1L, ReportActor.member(8L), request()));

        assertThat(exception.getErrorCode()).isEqualTo(ReportErrorCode.REPORT_ALREADY_FLAGGED);
    }

    @Test
    void guestCanFlagAnonymousReport() {
        when(reportRepository.getVisibleReportEntity(1L)).thenReturn(anonymousReport("author"));
        when(flagRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.flag(1L, ReportActor.anonymous("reporter"), request());

        verify(flagRepository).saveAndFlush(any(ReportFlag.class));
    }

    @Test
    void memberCanFlagAnonymousReport() {
        when(reportRepository.getVisibleReportEntity(1L)).thenReturn(anonymousReport("author"));
        when(flagRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.flag(1L, ReportActor.member(8L), request());

        verify(flagRepository).saveAndFlush(any(ReportFlag.class));
    }

    @Test
    void validFlagStoresNormalizedDetail() {
        when(reportRepository.getVisibleReportEntity(1L)).thenReturn(memberReport(7L));
        when(flagRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.flag(1L, ReportActor.anonymous("guest"),
                new CreateReportFlagRequest(ReportFlagReason.SPAM, "  반복 게시물  "));

        ArgumentCaptor<ReportFlag> captor = ArgumentCaptor.forClass(ReportFlag.class);
        verify(flagRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getDetail()).isEqualTo("반복 게시물");
    }

    private CreateReportFlagRequest request() {
        return new CreateReportFlagRequest(ReportFlagReason.ABUSE, "욕설");
    }

    private WeatherReport memberReport(Long memberId) {
        return WeatherReport.ofMember(
                1L, memberId, Temperature.FRESH, Precipitation.NONE, Sunlight.MODERATE, "맑아요");
    }

    private WeatherReport anonymousReport(String anonymousKey) {
        return WeatherReport.ofAnonymous(
                1L, anonymousKey, Temperature.FRESH, Precipitation.NONE, Sunlight.MODERATE, "맑아요");
    }
}
