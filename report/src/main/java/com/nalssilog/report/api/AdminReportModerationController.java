package com.nalssilog.report.api;

import com.nalssilog.report.api.dto.AdminActorRestrictionResponse;
import com.nalssilog.report.api.dto.AdminModerateReportRequest;
import com.nalssilog.report.api.dto.AdminModerationActionPageResponse;
import com.nalssilog.report.api.dto.AdminProcessFlagRequest;
import com.nalssilog.report.api.dto.AdminReportFlagPageResponse;
import com.nalssilog.report.api.dto.AdminReportModerationResponse;
import com.nalssilog.report.api.dto.AdminRestrictAuthorRequest;
import com.nalssilog.report.application.AdminModerationService;
import com.nalssilog.report.domain.ReportFlagStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminReportModerationController {

    private final AdminModerationService moderationService;

    @GetMapping("/report-flags")
    public AdminReportFlagPageResponse flags(
            @RequestParam(required = false) ReportFlagStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return moderationService.listFlags(status, page, size);
    }

    @PatchMapping("/report-flags/{flagId}")
    public AdminReportFlagPageResponse.Item processFlag(
            @PathVariable Long flagId,
            @AuthenticationPrincipal Long adminMemberId,
            @Valid @RequestBody AdminProcessFlagRequest request
    ) {
        return moderationService.processFlag(flagId, adminMemberId, request);
    }

    @PostMapping("/reports/{reportId}/moderation")
    public AdminReportModerationResponse moderate(
            @PathVariable Long reportId,
            @AuthenticationPrincipal Long adminMemberId,
            @Valid @RequestBody AdminModerateReportRequest request
    ) {
        return moderationService.moderate(reportId, adminMemberId, request);
    }

    @PostMapping("/reports/{reportId}/author-restriction")
    public AdminActorRestrictionResponse restrictAuthor(
            @PathVariable Long reportId,
            @AuthenticationPrincipal Long adminMemberId,
            @Valid @RequestBody AdminRestrictAuthorRequest request
    ) {
        return moderationService.restrictAuthor(reportId, adminMemberId, request);
    }

    @DeleteMapping("/reports/{reportId}/author-restriction")
    public AdminActorRestrictionResponse liftRestriction(
            @PathVariable Long reportId,
            @AuthenticationPrincipal Long adminMemberId
    ) {
        return moderationService.liftAuthorRestriction(reportId, adminMemberId);
    }

    @GetMapping("/moderation-actions")
    public AdminModerationActionPageResponse actions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return moderationService.listActions(page, size);
    }
}
