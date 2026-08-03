package com.nalssilog.report.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.member.domain.TermsType;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateReportRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validatesNestedTermAgreementFields() {
        CreateReportRequest request = request(List.of(
                new TermsAgreement(TermsType.SERVICE, " "),
                new TermsAgreement(TermsType.PRIVACY, "1.0")
        ));

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString())
                        .contains("agreedTerms"));
    }

    @Test
    void keepsTermsOptionalAtDtoLevelForMemberReports() {
        CreateReportRequest request = request(null);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.toCommand().agreedTerms()).isEmpty();
    }

    private CreateReportRequest request(List<TermsAgreement> agreedTerms) {
        return new CreateReportRequest(
                1L,
                Temperature.FRESH,
                Precipitation.NONE,
                Sunlight.MODERATE,
                "맑아요",
                List.of(),
                agreedTerms
        );
    }
}
