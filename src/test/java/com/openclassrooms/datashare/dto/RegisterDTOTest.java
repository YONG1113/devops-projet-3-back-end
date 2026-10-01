package com.openclassrooms.datashare.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterDTOTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRegisterRequestHasNoValidationErrors() {
        assertThat(validator.validate(validDto())).isEmpty();
    }

    @Test
    void blankRequiredFieldsAreRejected() {
        RegisterDTO dto = new RegisterDTO();

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("login", "password");
    }

    private RegisterDTO validDto() {
        RegisterDTO dto = new RegisterDTO();
        dto.setLogin("john");
        dto.setPassword("password");
        return dto;
    }
}
