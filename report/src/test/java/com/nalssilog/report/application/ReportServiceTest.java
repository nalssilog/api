package com.nalssilog.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.common.response.CursorPage;
import com.nalssilog.report.api.dto.ReportResponse;
import com.nalssilog.report.application.dto.AuthorInfo;
import com.nalssilog.report.application.dto.LocationSummary;
import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.application.dto.ReportData;
import com.nalssilog.report.application.dto.WeatherStatsData;
import com.nalssilog.report.client.ImageStorageClient;
import com.nalssilog.report.client.LocationClient;
import com.nalssilog.report.client.MemberClient;
import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import com.nalssilog.report.domain.WeatherReport;
import com.nalssilog.report.domain.event.ReportDeletedEvent;
import com.nalssilog.report.repository.ThanksRepository;
import com.nalssilog.report.repository.WeatherReportRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class ReportServiceTest {

    private final WeatherReportRepository reportRepository = mock(WeatherReportRepository.class);
    private final ThanksRepository thanksRepository = mock(ThanksRepository.class);
    private final MemberClient memberClient = mock(MemberClient.class);
    private final LocationClient locationClient = mock(LocationClient.class);
    private final ImageStorageClient imageStorageClient = mock(ImageStorageClient.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(
                reportRepository,
                thanksRepository,
                memberClient,
                locationClient,
                imageStorageClient,
                eventPublisher
        );
    }

    @Test
    void memberAuthorDeletesReportAndRelatedData() {
        WeatherReport report = memberReport(1L);

        report.addImages(List.of("reports/2026/07/one.jpg", "reports/2026/07/two.jpg"));
        when(reportRepository.getReportEntity(10L)).thenReturn(report);

        service.delete(10L, List.of(ReportActor.member(1L)));

        verify(thanksRepository).deleteAllByReportId(10L);
        verify(reportRepository).delete(report);

        ArgumentCaptor<ReportDeletedEvent> eventCaptor = ArgumentCaptor.forClass(ReportDeletedEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().imageKeys())
                .containsExactly("reports/2026/07/one.jpg", "reports/2026/07/two.jpg");
    }

    @Test
    void anonymousAuthorCanDeleteWithExistingAnonymousCookieIdentity() {
        WeatherReport report = anonymousReport("anonymous-key");

        when(reportRepository.getReportEntity(10L)).thenReturn(report);

        service.delete(10L, List.of(
                ReportActor.member(1L),
                ReportActor.anonymous("anonymous-key")
        ));

        verify(reportRepository).delete(report);
    }

    @Test
    void nonAuthorCannotDeleteReport() {
        WeatherReport report = memberReport(1L);
        List<ReportActor> actors = List.of(ReportActor.member(2L));

        when(reportRepository.getReportEntity(10L)).thenReturn(report);

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.delete(10L, actors)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ReportErrorCode.REPORT_DELETE_FORBIDDEN);
        verify(thanksRepository, never()).deleteAllByReportId(10L);
        verify(reportRepository, never()).delete(report);
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void detailMarksPreLoginAnonymousReportAsMineAfterLogin() {
        ReportData data = anonymousData("anonymous-key");

        when(reportRepository.getReport(10L)).thenReturn(data);
        when(locationClient.getLocation(1L)).thenReturn(location());

        ReportResponse response = service.get(
                10L,
                ReportActor.member(1L),
                List.of(ReportActor.member(1L), ReportActor.anonymous("anonymous-key"))
        );

        assertThat(response.isMine()).isTrue();
    }

    @Test
    void listIncludesOwnershipCalculatedFromAllAvailableActors() {
        ReportData data = anonymousData("anonymous-key");

        when(reportRepository.findPage(eq(1L), isNull(), isNull(), eq(21)))
                .thenReturn(List.of(data));
        when(locationClient.getLocation(1L)).thenReturn(location());
        when(thanksRepository.countByReportIds(List.of(10L))).thenReturn(Map.of());
        when(thanksRepository.thankedReportIds(List.of(10L), ReportActor.member(1L))).thenReturn(Set.of());

        CursorPage<ReportResponse> response = service.list(
                1L,
                null,
                ReportActor.member(1L),
                List.of(ReportActor.member(1L), ReportActor.anonymous("anonymous-key"))
        );

        assertThat(response.items()).singleElement()
                .extracting(ReportResponse::isMine)
                .isEqualTo(true);
    }

    @Test
    void listLoadsDistinctMemberAuthorsInOneBulkCall() {
        ReportData first = memberData(10L, 7L);
        ReportData second = memberData(11L, 8L);
        AuthorInfo firstAuthor = new AuthorInfo(7L, "first", null, null);
        AuthorInfo secondAuthor = new AuthorInfo(8L, "second", null, null);

        when(reportRepository.findPage(eq(1L), isNull(), isNull(), eq(21)))
                .thenReturn(List.of(first, second));
        when(locationClient.getLocation(1L)).thenReturn(location());
        when(thanksRepository.countByReportIds(List.of(10L, 11L))).thenReturn(Map.of());
        when(thanksRepository.thankedReportIds(List.of(10L, 11L), null)).thenReturn(Set.of());
        when(memberClient.findActiveAuthors(List.of(7L, 8L)))
                .thenReturn(Map.of(7L, firstAuthor, 8L, secondAuthor));

        CursorPage<ReportResponse> response = service.list(
                1L,
                null,
                null,
                List.of());

        assertThat(response.items())
                .extracting(item -> item.author().id())
                .containsExactly("7", "8");
        verify(memberClient).findActiveAuthors(List.of(7L, 8L));
        verify(memberClient, never()).findActiveAuthor(any());
    }

    @Test
    void statsAggregatesReportsFromTheLastThreeHours() {
        WeatherStatsData stats = new WeatherStatsData(0L, Map.of(), Map.of(), Map.of());

        when(locationClient.getLocation(1L)).thenReturn(location());
        when(reportRepository.statsSince(eq(1L), any(Instant.class)))
                .thenReturn(stats);
        Instant lowerBound = Instant.now().minusSeconds(3 * 60 * 60);

        service.stats(1L);

        ArgumentCaptor<Instant> sinceCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(reportRepository).statsSince(eq(1L), sinceCaptor.capture());
        assertThat(sinceCaptor.getValue())
                .isBetween(lowerBound, Instant.now().minusSeconds(3 * 60 * 60));
    }

    private WeatherReport memberReport(Long memberId) {
        return WeatherReport.ofMember(
                1L, memberId, Temperature.FRESH, Precipitation.NONE, Sunlight.MODERATE, "맑아요");
    }

    private WeatherReport anonymousReport(String anonymousKey) {
        return WeatherReport.ofAnonymous(
                1L, anonymousKey, Temperature.FRESH, Precipitation.NONE, Sunlight.MODERATE, "맑아요");
    }

    private ReportData anonymousData(String anonymousKey) {
        return new ReportData(
                10L,
                1L,
                ActorType.ANONYMOUS,
                null,
                anonymousKey,
                Temperature.FRESH,
                Precipitation.NONE,
                Sunlight.MODERATE,
                "맑아요",
                List.of(),
                Instant.parse("2026-07-24T00:00:00Z")
        );
    }

    private ReportData memberData(Long reportId, Long memberId) {
        return new ReportData(
                reportId,
                1L,
                ActorType.MEMBER,
                memberId,
                null,
                Temperature.FRESH,
                Precipitation.NONE,
                Sunlight.MODERATE,
                "맑아요",
                List.of(),
                Instant.parse("2026-07-24T00:00:00Z")
        );
    }

    private LocationSummary location() {
        return new LocationSummary(
                1L, "서울특별시", "강남구", "역삼동", "서울특별시 강남구 역삼동", "강남구 역삼동");
    }
}
