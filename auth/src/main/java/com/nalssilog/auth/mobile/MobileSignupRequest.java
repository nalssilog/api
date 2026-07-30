package com.nalssilog.auth.mobile;

import com.nalssilog.member.application.dto.TermsAgreement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MobileSignupRequest(
        @NotBlank @Size(max = 200) String signupTicket,
        @NotNull List<@Valid @NotNull TermsAgreement> agreedTerms,
        @Valid @NotNull MobileDeviceRequest device
) {
}
