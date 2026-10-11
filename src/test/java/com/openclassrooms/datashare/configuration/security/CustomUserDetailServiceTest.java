package com.openclassrooms.datashare.configuration.security;

import com.openclassrooms.datashare.entities.User;
import com.openclassrooms.datashare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailService userDetailService;

    @Test
    void loadUserByUsernameReturnsTheMatchingUser() {
        User user = new User();
        user.setLogin("john@example.com");
        user.setPassword("encoded-password");
        when(userRepository.findByLogin("john@example.com")).thenReturn(Optional.of(user));

        var result = userDetailService.loadUserByUsername("john@example.com");

        assertThat(result).isSameAs(user);
        assertThat(result.getUsername()).isEqualTo("john@example.com");
        verify(userRepository).findByLogin("john@example.com");
    }

    @Test
    void loadUserByUsernameThrowsWhenTheUserDoesNotExist() {
        when(userRepository.findByLogin("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User Not Found with username: unknown@example.com");

        verify(userRepository).findByLogin("unknown@example.com");
    }
}
