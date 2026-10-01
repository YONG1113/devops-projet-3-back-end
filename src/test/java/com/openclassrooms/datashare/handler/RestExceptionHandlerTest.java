package com.openclassrooms.datashare.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.nio.file.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    private RestExceptionHandler handler;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        handler = new RestExceptionHandler();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/api/test");
        request = new ServletWebRequest(servletRequest);
    }

    @Test
    void illegalArgumentReturnsBadRequest() {
        ResponseEntity<Object> response =
                handler.handleConflict(new IllegalArgumentException("Invalid value"), request);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid value");
    }

    @Test
    void badCredentialsReturnsUnauthorized() {
        ResponseEntity<Object> response =
                handler.handleAuthenticationException(
                        new BadCredentialsException("Invalid credentials"), request);

        assertErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @Test
    void accessDeniedReturnsForbidden() {
        ResponseEntity<Object> response =
                handler.handleForbiddenException(
                        new AccessDeniedException("Access denied"), request);

        assertErrorResponse(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    @Test
    void unexpectedExceptionReturnsInternalServerErrorWithoutExposingDetails() {
        ResponseEntity<Object> response =
                handler.handleException(new RuntimeException("Database password"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Internal Server error");
    }

    private void assertErrorResponse(
            ResponseEntity<Object> response,
            HttpStatus expectedStatus,
            String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isInstanceOf(ErrorDetails.class);

        ErrorDetails error = (ErrorDetails) response.getBody();
        assertThat(error.getTimestamp()).isNotNull();
        assertThat(error.getMessage()).isEqualTo(expectedMessage);
        assertThat(error.getDetails()).contains("/api/test");
    }
}
