package com.openclassrooms.datashare.entities;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validUserHasNoValidationErrors() {
        assertThat(validator.validate(validUser())).isEmpty();
    }

    @Test
    void blankRequiredFieldsAreRejected() {
        User user = validUser();
        user.setFirstName("");
        user.setLastName(" ");
        user.setLogin(null);
        user.setPassword("");

        assertThat(validator.validate(user))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("firstName", "lastName", "login", "password");
    }

    @Test
    void userDetailsMethodsDescribeAnEnabledUser() {
        User user = validUser();

        assertThat(user.getUsername()).isEqualTo("john");
        assertThat(user.getAuthorities()).isEmpty();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    private User validUser() {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setLogin("john");
        user.setPassword("encoded-password");
        return user;
    }
}
