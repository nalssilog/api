package com.nalssilog.report.api;

import com.nalssilog.common.response.CursorPage;
import com.nalssilog.report.api.dto.CreateReportRequest;
import com.nalssilog.report.api.dto.ReportResponse;
import com.nalssilog.report.api.dto.ThanksResponse;
import com.nalssilog.report.api.dto.WeatherStatsResponse;
import com.nalssilog.report.application.ReportService;
import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.config.ReportActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportActorResolver actorResolver;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@AuthenticationPrincipal Long memberId,
                                 @Valid @RequestBody CreateReportRequest request,
                                 HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        ReportActor actor = actorResolver.resolveForWrite(memberId, httpRequest, httpResponse);

        return reportService.create(actor, request.toCommand());
    }

    @GetMapping
    public CursorPage<ReportResponse> list(@RequestParam Long locationId,
                                           @RequestParam(required = false) String cursor,
                                           @AuthenticationPrincipal Long memberId,
                                           HttpServletRequest httpRequest) {
        ReportActor viewer = actorResolver.resolveForRead(memberId, httpRequest);

        return reportService.list(locationId, cursor, viewer);
    }

    /**
     * 내 제보 목록(로그인 회원 전용). /{id} 보다 먼저 매칭되도록 위에 둔다.
     */
    @GetMapping("/me")
    public CursorPage<ReportResponse> myReports(@AuthenticationPrincipal Long memberId,
                                                @RequestParam(required = false) String cursor,
                                                HttpServletRequest httpRequest) {
        ReportActor viewer = actorResolver.resolveForRead(memberId, httpRequest);

        return reportService.listByMember(memberId, cursor, viewer);
    }

    /**
     * 특정 회원의 제보 목록(공개). 탈퇴 회원이면 작성자는 "익명 이웃"으로 렌더된다.
     */
    @GetMapping("/members/{memberId}")
    public CursorPage<ReportResponse> memberReports(@PathVariable Long memberId,
                                                    @RequestParam(required = false) String cursor,
                                                    @AuthenticationPrincipal Long viewerMemberId,
                                                    HttpServletRequest httpRequest) {
        ReportActor viewer = actorResolver.resolveForRead(viewerMemberId, httpRequest);

        return reportService.listByMember(memberId, cursor, viewer);
    }

    /**
     * 지역 날씨 통계(최근 24시간 3축 분포 + 제보 수). /{id} 보다 먼저 매칭되도록 위에 둔다.
     */
    @GetMapping("/stats")
    public WeatherStatsResponse stats(@RequestParam Long locationId) {
        return reportService.stats(locationId);
    }

    @GetMapping("/{id}")
    public ReportResponse detail(@PathVariable Long id,
                                 @AuthenticationPrincipal Long memberId,
                                 HttpServletRequest httpRequest) {
        ReportActor viewer = actorResolver.resolveForRead(memberId, httpRequest);

        return reportService.get(id, viewer);
    }

    @PostMapping("/{id}/thanks")
    public ThanksResponse addThanks(@PathVariable Long id,
                                    @AuthenticationPrincipal Long memberId,
                                    HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        ReportActor actor = actorResolver.resolveForWrite(memberId, httpRequest, httpResponse);

        return reportService.addThanks(id, actor);
    }

    @DeleteMapping("/{id}/thanks")
    public ThanksResponse removeThanks(@PathVariable Long id,
                                       @AuthenticationPrincipal Long memberId,
                                       HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        ReportActor actor = actorResolver.resolveForWrite(memberId, httpRequest, httpResponse);

        return reportService.removeThanks(id, actor);
    }
}
