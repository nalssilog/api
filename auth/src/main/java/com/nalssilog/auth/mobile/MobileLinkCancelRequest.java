package com.nalssilog.auth.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MobileLinkCancelRequest(
        @NotBlank @Size(max = 200) String linkTicket
) {
}
