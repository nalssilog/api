package com.nalssilog.report.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.api.dto.CreateReportFlagRequest;
import com.nalssilog.report.api.dto.ReportFlagResponse;
import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.report.domain.ReportFlag;
import com.nalssilog.report.domain.WeatherReport;
import com.nalssilog.report.repository.ReportFlagJpaRepository;
import com.nalssilog.report.repository.WeatherReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportFlagService {

    private final WeatherReportRepository reportRepository;
    private final ReportFlagJpaRepository flagRepository;

    @Transactional
    public ReportFlagResponse flag(
            Long reportId,
            ReportActor reporter,
            CreateReportFlagRequest request
    ) {
        WeatherReport report = reportRepository.getVisibleReportEntity(reportId);
        ReportActor author = ReportActor.authorOf(report);

        if (sameActor(author, reporter)) {
            throw new NalssiLogException(ReportErrorCode.CANNOT_FLAG_OWN_REPORT);
        }

        if (flagRepository.existsByReport_IdAndReporterTypeAndReporterKey(
                reportId, reporter.type(), reporter.actorKey())) {
            throw new NalssiLogException(ReportErrorCode.REPORT_ALREADY_FLAGGED);
        }

        try {
            ReportFlag saved = flagRepository.saveAndFlush(ReportFlag.create(
                    report, reporter, request.reason(), normalize(request.detail())));

            return ReportFlagResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new NalssiLogException(ReportErrorCode.REPORT_ALREADY_FLAGGED);
        }
    }

    private boolean sameActor(ReportActor first, ReportActor second) {
        return first.type() == second.type() && first.actorKey().equals(second.actorKey());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }
}
