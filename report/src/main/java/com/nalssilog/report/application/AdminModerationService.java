package com.nalssilog.report.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.api.dto.AdminActorRestrictionResponse;
import com.nalssilog.report.api.dto.AdminModerateReportRequest;
import com.nalssilog.report.api.dto.AdminModerationActionPageResponse;
import com.nalssilog.report.api.dto.AdminProcessFlagRequest;
import com.nalssilog.report.api.dto.AdminReportFlagPageResponse;
import com.nalssilog.report.api.dto.AdminReportModerationResponse;
import com.nalssilog.report.api.dto.AdminRestrictAuthorRequest;
import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.client.ImageStorageClient;
import com.nalssilog.report.domain.ActorRestriction;
import com.nalssilog.report.domain.ModerationAction;
import com.nalssilog.report.domain.ModerationActionType;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.report.domain.ReportFlag;
import com.nalssilog.report.domain.ReportFlagStatus;
import com.nalssilog.report.domain.WeatherReport;
import com.nalssilog.report.domain.WeatherReportImage;
import com.nalssilog.report.repository.ActorRestrictionJpaRepository;
import com.nalssilog.report.repository.ModerationActionJpaRepository;
import com.nalssilog.report.repository.ReportFlagJpaRepository;
import com.nalssilog.report.repository.WeatherReportRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminModerationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final WeatherReportRepository reportRepository;
    private final ReportFlagJpaRepository flagRepository;
    private final ActorRestrictionJpaRepository restrictionRepository;
    private final ModerationActionJpaRepository actionRepository;
    private final ActorRestrictionService restrictionService;
    private final ImageStorageClient imageStorageClient;

    public AdminReportFlagPageResponse listFlags(
            ReportFlagStatus status,
            int page,
            int size
    ) {
        PageRequest pageable = pageRequest(page, size);
        Page<ReportFlag> flags = status == null
                ? flagRepository.findAllByOrderByCreatedAtAsc(pageable)
                : flagRepository.findAllByStatusOrderByCreatedAtAsc(status, pageable);

        return new AdminReportFlagPageResponse(
                flags.getContent().stream().map(this::flagItem).toList(),
                flags.getNumber(), flags.getSize(), flags.getTotalElements(), flags.getTotalPages());
    }

    @Transactional
    public AdminReportFlagPageResponse.Item processFlag(
            Long flagId,
            Long adminMemberId,
            AdminProcessFlagRequest request
    ) {
        ReportFlag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new NalssiLogException(ReportErrorCode.REPORT_FLAG_NOT_FOUND));

        flag.process(request.status(), adminMemberId, normalize(request.note()));
        actionRepository.save(ModerationAction.create(
                request.status() == ReportFlagStatus.RESOLVED
                        ? ModerationActionType.FLAG_RESOLVED
                        : ModerationActionType.FLAG_REJECTED,
                adminMemberId,
                flag.getReport().getId(),
                ReportActor.authorOf(flag.getReport()),
                normalizeOrDefault(request.note(), "신고 처리")));

        return flagItem(flag);
    }

    @Transactional
    public AdminReportModerationResponse moderate(
            Long reportId,
            Long adminMemberId,
            AdminModerateReportRequest request
    ) {
        WeatherReport report = reportRepository.getReportEntity(reportId);
        ModerationActionType actionType = switch (request.action()) {
            case HIDE -> {
                report.hide();
                yield ModerationActionType.REPORT_HIDDEN;
            }
            case RESTORE -> {
                report.restore();
                yield ModerationActionType.REPORT_RESTORED;
            }
            case REMOVE -> {
                report.removeByModerator();
                yield ModerationActionType.REPORT_REMOVED;
            }
        };

        if (request.action() != com.nalssilog.report.domain.ReportModerationCommand.RESTORE) {
            flagRepository.findAllByReport_IdAndStatus(reportId, ReportFlagStatus.PENDING)
                    .forEach(flag -> flag.process(
                            ReportFlagStatus.RESOLVED,
                            adminMemberId,
                            "콘텐츠 운영 조치: " + request.reason().strip()));
        }

        actionRepository.save(ModerationAction.create(
                actionType,
                adminMemberId,
                reportId,
                ReportActor.authorOf(report),
                request.reason().strip()));

        return new AdminReportModerationResponse(
                String.valueOf(reportId), report.getModerationStatus());
    }

    @Transactional
    public AdminActorRestrictionResponse restrictAuthor(
            Long reportId,
            Long adminMemberId,
            AdminRestrictAuthorRequest request
    ) {
        if (request.expiresAt() != null && !request.expiresAt().isAfter(Instant.now())) {
            throw new NalssiLogException(ReportErrorCode.INVALID_RESTRICTION_EXPIRY);
        }

        WeatherReport report = reportRepository.getReportEntity(reportId);
        ReportActor author = ReportActor.authorOf(report);

        if (restrictionService.findActive(author).isPresent()) {
            throw new NalssiLogException(ReportErrorCode.AUTHOR_ALREADY_RESTRICTED);
        }

        ActorRestriction restriction = restrictionRepository.save(ActorRestriction.create(
                author,
                reportId,
                request.reason().strip(),
                request.expiresAt(),
                adminMemberId));

        actionRepository.save(ModerationAction.create(
                ModerationActionType.AUTHOR_RESTRICTED,
                adminMemberId,
                reportId,
                author,
                request.reason().strip()));

        return AdminActorRestrictionResponse.from(restriction);
    }

    @Transactional
    public AdminActorRestrictionResponse liftAuthorRestriction(
            Long reportId,
            Long adminMemberId
    ) {
        WeatherReport report = reportRepository.getReportEntity(reportId);
        ReportActor author = ReportActor.authorOf(report);
        ActorRestriction restriction = restrictionService.findActive(author)
                .orElseThrow(() -> new NalssiLogException(
                        ReportErrorCode.AUTHOR_RESTRICTION_NOT_FOUND));

        restriction.lift(adminMemberId);
        actionRepository.save(ModerationAction.create(
                ModerationActionType.AUTHOR_RESTRICTION_LIFTED,
                adminMemberId,
                reportId,
                author,
                "관리자 작성 제한 해제"));

        return AdminActorRestrictionResponse.from(restriction);
    }

    public AdminModerationActionPageResponse listActions(int page, int size) {
        Page<ModerationAction> actions = actionRepository.findAllByOrderByCreatedAtDesc(
                pageRequest(page, size));

        List<AdminModerationActionPageResponse.Item> items = actions.getContent().stream()
                .map(action -> new AdminModerationActionPageResponse.Item(
                        String.valueOf(action.getId()),
                        action.getActionType(),
                        String.valueOf(action.getAdminMemberId()),
                        action.getReportId() == null ? null : String.valueOf(action.getReportId()),
                        action.getTargetActorType(),
                        action.getReason(),
                        action.getCreatedAt()))
                .toList();

        return new AdminModerationActionPageResponse(
                items,
                actions.getNumber(), actions.getSize(),
                actions.getTotalElements(), actions.getTotalPages());
    }

    private AdminReportFlagPageResponse.Item flagItem(ReportFlag flag) {
        WeatherReport report = flag.getReport();
        List<String> imageUrls = report.getImages().stream()
                .sorted(Comparator.comparingInt(WeatherReportImage::getDisplayOrder))
                .map(WeatherReportImage::getStorageKey)
                .map(imageStorageClient::toPublicUrl)
                .toList();
        AdminReportFlagPageResponse.Report reportItem = new AdminReportFlagPageResponse.Report(
                String.valueOf(report.getLocationId()),
                report.getAuthorType(),
                report.getAuthorMemberId() == null ? null : String.valueOf(report.getAuthorMemberId()),
                report.getComment(),
                imageUrls,
                report.getModerationStatus(),
                report.getCreatedAt());

        return new AdminReportFlagPageResponse.Item(
                String.valueOf(flag.getId()),
                String.valueOf(report.getId()),
                flag.getReason(),
                flag.getDetail(),
                flag.getStatus(),
                flag.getReporterType(),
                flag.getCreatedAt(),
                flag.getProcessedAt(),
                flag.getProcessedByMemberId() == null
                        ? null
                        : String.valueOf(flag.getProcessedByMemberId()),
                flag.getResolutionNote(),
                reportItem);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String normalizeOrDefault(String value, String fallback) {
        String normalized = normalize(value);

        return normalized == null ? fallback : normalized;
    }
}
