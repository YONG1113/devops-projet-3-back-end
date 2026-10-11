package com.openclassrooms.datashare.controller;

import com.openclassrooms.datashare.dto.LoginRequestDTO;
import com.openclassrooms.datashare.dto.RegisterDTO;
import com.openclassrooms.datashare.entities.User;
import com.openclassrooms.datashare.mapper.UserDtoMapper;
import com.openclassrooms.datashare.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock UserService userService;
    @Mock UserDtoMapper userDtoMapper;
    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService, userDtoMapper);
    }

    @Test
    void registerMapsAndPassesTheUserToTheService() {
        RegisterDTO request = new RegisterDTO();
        request.setLogin("user@example.com");
        request.setPassword("password");
        User user = new User();
        when(userDtoMapper.toEntity(request)).thenReturn(user);

        var response = controller.register(request);

        verify(userService).register(user);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void loginReturnsTheGeneratedToken() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setLogin("user@example.com");
        request.setPassword("password");
        when(userService.login(request.getLogin(), request.getPassword())).thenReturn("jwt-token");

        var response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("jwt-token");
    }

    @Test
    void currentUserReturnsTheAuthenticatedUserId() {
        User user = new User();
        user.setId(42L);

        assertThat(controller.currentUser(user)).containsEntry("id", "42");
        verifyNoInteractions(userService, userDtoMapper);
    }
}
