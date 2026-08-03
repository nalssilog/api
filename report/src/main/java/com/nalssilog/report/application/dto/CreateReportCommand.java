package com.nalssilog.report.application.dto;

import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import java.util.List;

public record CreateReportCommand(
        Long locationId,
        Temperature temperature,
        Precipitation precipitation,
        Sunlight sunlight,
        String comment,
        List<String> imageKeys,
        List<TermsAgreement> agreedTerms
) {
}
