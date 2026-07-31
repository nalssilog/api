package com.nalssilog.common.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960")
class RequestLoggingFilterTest {

    @Test
    void masksOAuthAndMobileCredentialQueryParameters() {
        String sanitized = RequestLoggingFilter.sanitizeQuery(
                "code=provider-code&state=app-state&code_challenge=challenge-value&locationId=10");

        assertThat(sanitized)
                .isEqualTo("code=***&state=***&code_challenge=***&locationId=10")
                .doesNotContain("provider-code", "app-state", "challenge-value");
    }

    @Test
    void recognizesEncodedSensitiveParameterName() {
        assertThat(RequestLoggingFilter.sanitizeQuery("co%64e=secret"))
                .isEqualTo("co%64e=***");
    }
}
