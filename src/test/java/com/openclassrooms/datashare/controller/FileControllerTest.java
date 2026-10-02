package com.openclassrooms.datashare.controller;

import com.openclassrooms.datashare.configuration.security.CustomUserDetailService;
import com.openclassrooms.datashare.configuration.security.JwtAuthenticationFilter;
import com.openclassrooms.datashare.configuration.security.SpringSecurityConfig;
import com.openclassrooms.datashare.service.FileService;
import com.openclassrooms.datashare.service.JwtService;
import com.openclassrooms.datashare.service.SupabaseStorageService;
import com.openclassrooms.datashare.repository.UserRepository;
import com.openclassrooms.datashare.repository.FileRepository;
import com.openclassrooms.datashare.entities.File;
import org.junit.jupiter.api.BeforeEach;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FileController.class, properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "jwt.expiration-ms=3600000"
})
@Import({ SpringSecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, FileService.class })
class FileControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailService userDetailsService;
    @MockitoSpyBean
    private FileService fileService;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private FileRepository fileRepository;
    @MockitoBean
    private SupabaseStorageService storageService;

    @BeforeEach
    void prepareStorage() {
        var account = new com.openclassrooms.datashare.entities.User();
        account.setId(42L);
        account.setLogin("user@example.com");
        when(userRepository.findByLogin("user@example.com")).thenReturn(Optional.of(account));
        when(storageService.getBucket()).thenReturn("files");
        when(fileRepository.saveAndFlush(any(File.class))).thenAnswer(invocation -> {
            File record = invocation.getArgument(0);
            record.setId(1L);
            return record;
        });
    }

    private final UserDetails user = User.withUsername("user@example.com")
            .password("encoded-password").authorities("USER").build();

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "hello.txt", "text/plain", new byte[] { 1, 2, 3 });
    }

    private String token() {
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
        return "Bearer " + jwtService.generateToken(user);
    }

    // @Test
    // void validJwtPassesFileAndAuthenticatedUserToService() throws Exception {
    // mockMvc.perform(multipart("/api/file").file(file())
    // .param("login", "someone-else@example.com")
    // .header("Authorization", token()))
    // .andExpect(status().isOk())
    // .andExpect(jsonPath("$.filename").value("hello.txt"))
    // .andExpect(jsonPath("$.size").value(3))
    // .andExpect(jsonPath("$.contentType").value("text/plain"))
    // .andExpect(jsonPath("$.status").value("stored"))
    // .andExpect(jsonPath("$.userId").value(42));
    // verify(fileService).upload(any(), eq(user.getUsername()));
    // }

    // @Test
    // void missingJwtIsRejectedBeforeService() throws Exception {
    // mockMvc.perform(multipart("/api/file").file(file())).andExpect(status().isUnauthorized());
    // verifyNoInteractions(fileService);
    // }

    // @Test
    // void matchingUserIdIsAccepted() throws Exception {
    // String authorization = token();
    // com.openclassrooms.datashare.entities.User account = new
    // com.openclassrooms.datashare.entities.User();
    // account.setId(42L);
    // account.setLogin(user.getUsername());
    // when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(account);

    // mockMvc.perform(multipart("/api/file").file(file()).param("userId", "42")
    // .header("Authorization", authorization))
    // .andExpect(status().isOk());
    // verify(fileService).upload(any(), eq(user.getUsername()));
    // }

    // @Test
    // void anotherUserIdIsRejected() throws Exception {
    // String authorization = token();
    // com.openclassrooms.datashare.entities.User account = new
    // com.openclassrooms.datashare.entities.User();
    // account.setId(42L);
    // account.setLogin(user.getUsername());
    // when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(account);

    // mockMvc.perform(multipart("/api/file").file(file()).param("userId", "99")
    // .header("Authorization", authorization))
    // .andExpect(status().isForbidden());
    // verifyNoInteractions(fileService);
    // }

    // @Test
    // void invalidJwtIsRejectedBeforeService() throws Exception {
    // mockMvc.perform(multipart("/api/file").file(file()).header("Authorization",
    // "Bearer invalid"))
    // .andExpect(status().isUnauthorized());
    // verifyNoInteractions(fileService);
    // }

    // @Test
    // void expiredJwtIsRejectedBeforeService() throws Exception {
    // JwtService expired = new
    // JwtService("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", -60000);
    // mockMvc.perform(multipart("/api/file").file(file())
    // .header("Authorization", "Bearer " + expired.generateToken(user)))
    // .andExpect(status().isUnauthorized());
    // verifyNoInteractions(fileService);
    // }

    // @Test
    // void missingFileIsRejected() throws Exception {
    // mockMvc.perform(multipart("/api/file").header("Authorization", token()))
    // .andExpect(status().isBadRequest());
    // verifyNoInteractions(fileService);
    // }

    // @Test
    // void emptyFileIsRejectedByService() throws Exception {
    // mockMvc.perform(multipart("/api/file")
    // .file(new MockMultipartFile("file", new byte[0]))
    // .header("Authorization", token()))
    // .andExpect(status().isBadRequest());
    // verify(fileService).upload(any(), eq(user.getUsername()));
    // }
}
