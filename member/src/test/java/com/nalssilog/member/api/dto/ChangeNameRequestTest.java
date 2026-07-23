package com.nalssilog.member.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class ChangeNameRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void stripsSurroundingWhitespaceBeforeValidation() {
        ChangeNameRequest request = new ChangeNameRequest("  홍길동  ");

        assertThat(request.name()).isEqualTo("홍길동");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankName() {
        ChangeNameRequest request = new ChangeNameRequest("   ");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsNameLongerThanThirtyCharacters() {
        ChangeNameRequest request = new ChangeNameRequest("가".repeat(31));

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
