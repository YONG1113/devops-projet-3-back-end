package com.openclassrooms.datashare.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestDTOTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validLoginRequestHasNoValidationErrors() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setLogin("john");
        dto.setPassword("password");

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void blankLoginAndPasswordAreRejected() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setLogin(" ");
        dto.setPassword("");

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("login", "password");
    }
}
