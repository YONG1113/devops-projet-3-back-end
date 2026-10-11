package com.openclassrooms.datashare.controller;

import com.openclassrooms.datashare.dto.LoginRequestDTO;
import com.openclassrooms.datashare.dto.RegisterDTO;
import com.openclassrooms.datashare.entities.User;
import com.openclassrooms.datashare.mapper.UserDtoMapper;
import com.openclassrooms.datashare.repository.UserRepository;
import com.openclassrooms.datashare.service.UserService;
import com.openclassrooms.datashare.support.PostgreSQLIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class IntegrationTests extends PostgreSQLIntegrationTest {

        @Autowired MockMvc mockMvc;
        @Autowired UserRepository integrationUserRepository;
        @Autowired PasswordEncoder integrationPasswordEncoder;

        @Test
        void integrationTestRegistersAndLogsInAUser() throws Exception {
            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"login":"integration@example.com","password":"password123"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            var storedUser = integrationUserRepository.findByLogin("integration@example.com").orElseThrow();
            assertThat(storedUser.getFirstName()).isNull();
            assertThat(storedUser.getLastName()).isNull();
            assertThat(integrationPasswordEncoder.matches("password123", storedUser.getPassword())).isTrue();

            String token = mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"login":"integration@example.com","password":"password123"}
                                    """))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(token).isNotBlank();
            mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(storedUser.getId().toString()));
        }

        @Test
        void integrationTestRejectsLoginWithAnIncorrectPassword() throws Exception {
            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"login":"wrong-password@example.com","password":"password123"}
                                    """))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"login":"wrong-password@example.com","password":"incorrect"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }
}
