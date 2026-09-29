package com.nexo.backend.validation;

import com.nexo.backend.dto.CustomerRequestDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidContactTest {

    private final ValidContactValidator validator = new ValidContactValidator();

    @Test
    void isValid_onlyEmail_returnsTrue() {
        assertThat(validator.isValid(new CustomerRequestDto("Ana", "ana@nexo.com", null, null), null)).isTrue();
    }

    @Test
    void isValid_onlyPhone_returnsTrue() {
        assertThat(validator.isValid(new CustomerRequestDto("Ana", null, "600123123", null), null)).isTrue();
    }

    @Test
    void isValid_bothPresent_returnsTrue() {
        assertThat(validator.isValid(new CustomerRequestDto("Ana", "ana@nexo.com", "600123123", null), null))
                .isTrue();
    }

    @Test
    void isValid_bothNull_returnsFalse() {
        assertThat(validator.isValid(new CustomerRequestDto("Ana", null, null, null), null)).isFalse();
    }

    @Test
    void isValid_bothEmpty_returnsFalse() {
        assertThat(validator.isValid(new CustomerRequestDto("Ana", "", "", null), null)).isFalse();
    }

    @Test
    void isValid_onlySpaces_returnsFalse() {
        assertThat(validator.isValid(new CustomerRequestDto("Ana", "   ", "   ", null), null)).isFalse();
    }
}
