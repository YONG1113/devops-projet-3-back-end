package com.openclassrooms.datashare.configuration.security;

import com.openclassrooms.datashare.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailService userService;
    @Mock private FilterChain chain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final UserDetails user = User.withUsername("john")
            .password("password").authorities("ROLE_USER").build();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, userService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer token");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic credentials", "Bearer", "bearer token"})
    void missingOrUnsupportedAuthorizationContinuesWithoutAuthentication(String header) throws Exception {
        request.removeHeader("Authorization");
        if (header != null) request.addHeader("Authorization", header);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingUsernameDoesNotLoadUser() throws Exception {
        when(jwtService.extractUsername("token")).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userService);
        verify(jwtService, never()).isTokenValid(anyString(), any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void existingAuthenticationIsPreserved() throws Exception {
        var existing = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existing);
        when(jwtService.extractUsername("token")).thenReturn("john");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verifyNoInteractions(userService);
        verify(jwtService, never()).isTokenValid(anyString(), any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void validTokenAuthenticatesUserBeforeContinuingChain() throws Exception {
        when(jwtService.extractUsername("token")).thenReturn("john");
        when(userService.loadUserByUsername("john")).thenReturn(user);
        when(jwtService.isTokenValid("token", user)).thenReturn(true);
        doAnswer(invocation -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.isAuthenticated()).isTrue();
            assertThat(authentication.getPrincipal()).isSameAs(user);
            assertThat(authentication.getCredentials()).isNull();
            assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
            assertThat(authentication.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidTokenDoesNotAuthenticateUser() throws Exception {
        when(jwtService.extractUsername("token")).thenReturn("john");
        when(userService.loadUserByUsername("john")).thenReturn(user);
        when(jwtService.isTokenValid("token", user)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void malformedTokenClearsExistingAuthenticationAndContinues() throws Exception {
        assertExtractionFailureClearsContext(new JwtException("Malformed token"));
    }

    @Test
    void illegalTokenClearsExistingAuthenticationAndContinues() throws Exception {
        assertExtractionFailureClearsContext(new IllegalArgumentException("Empty token"));
    }

    private void assertExtractionFailureClearsContext(RuntimeException exception) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(jwtService.extractUsername("token")).thenThrow(exception);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void unknownUserContinuesWithoutAuthentication() throws Exception {
        when(jwtService.extractUsername("token")).thenReturn("john");
        when(userService.loadUserByUsername("john"))
                .thenThrow(new UsernameNotFoundException("Unknown user"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).isTokenValid(anyString(), any());
        verify(chain).doFilter(request, response);
    }
}
