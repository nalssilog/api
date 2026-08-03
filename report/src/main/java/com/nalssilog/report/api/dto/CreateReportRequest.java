package com.nalssilog.report.api.dto;

import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.report.application.dto.CreateReportCommand;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * imageKeys 는 presigned 업로드 후 받은 storage_key 목록(최대 3장).
 * 체감 3축: temperature(COLD/FRESH/HOT), precipitation(NONE/LIGHT/HEAVY), sunlight(LOW/MODERATE/STRONG).
 * 제품 정책: comment 필수(1~100자), 이미지 최대 3장.
 * 비회원 제보는 agreedTerms 에 SERVICE, PRIVACY 동의를 각각 문서 버전과 함께 전달해야 한다.
 */
public record CreateReportRequest(
        @NotNull(message = "지역을 선택해 주세요.")
        Long locationId,

        @NotNull(message = "체감 온도를 선택해 주세요.")
        Temperature temperature,

        @NotNull(message = "체감 강수량을 선택해 주세요.")
        Precipitation precipitation,

        @NotNull(message = "체감 햇빛 밝기를 선택해 주세요.")
        Sunlight sunlight,

        @NotBlank(message = "한마디를 입력해 주세요.")
        @Size(min = 1, max = 100, message = "한마디는 1~100자여야 합니다.")
        String comment,

        List<@NotBlank String> imageKeys,

        List<@Valid @NotNull(message = "약관 동의 항목이 비어 있습니다.") TermsAgreement> agreedTerms
) {

    public CreateReportCommand toCommand() {
        return new CreateReportCommand(locationId, temperature, precipitation, sunlight, comment,
                imageKeys == null ? List.of() : List.copyOf(imageKeys),
                agreedTerms == null ? List.of() : List.copyOf(agreedTerms));
    }
}
