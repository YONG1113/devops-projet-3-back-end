package com.openclassrooms.datashare.handler;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorDetailsTest {

    @Test
    void allArgumentsConstructorStoresErrorInformation() {
        LocalDateTime timestamp = LocalDateTime.now();

        ErrorDetails error = new ErrorDetails(timestamp, "Invalid value", "uri=/api/test");

        assertThat(error.getTimestamp()).isEqualTo(timestamp);
        assertThat(error.getMessage()).isEqualTo("Invalid value");
        assertThat(error.getDetails()).isEqualTo("uri=/api/test");
    }

    @Test
    void settersPopulateAnEmptyErrorDetails() {
        LocalDateTime timestamp = LocalDateTime.now();
        ErrorDetails error = new ErrorDetails();

        error.setTimestamp(timestamp);
        error.setMessage("Not found");
        error.setDetails("uri=/api/test");

        assertThat(error)
                .extracting(ErrorDetails::getTimestamp,
                        ErrorDetails::getMessage,
                        ErrorDetails::getDetails)
                .containsExactly(timestamp, "Not found", "uri=/api/test");
    }
}
