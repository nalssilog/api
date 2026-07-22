package com.nalssilog.report.api.dto;

import com.nalssilog.report.application.dto.CreateReportCommand;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * imageKeys 는 presigned 업로드 후 받은 storage_key 목록(최대 3장).
 * 체감 3축: temperature(COLD/FRESH/HOT), precipitation(NONE/LIGHT/HEAVY), sunlight(LOW/MODERATE/STRONG).
 * 제품 정책: comment 필수(1~100자), 이미지 최대 3장.
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

        List<@NotBlank String> imageKeys
) {

    public CreateReportCommand toCommand() {
        return new CreateReportCommand(locationId, temperature, precipitation, sunlight, comment,
                imageKeys == null ? List.of() : imageKeys);
    }
}
