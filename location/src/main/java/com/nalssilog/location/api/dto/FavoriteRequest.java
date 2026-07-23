package com.nalssilog.location.api.dto;

import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(
        @NotNull(message = "지역을 선택해 주세요.")
        Long locationId
) {
}
