package com.openclassrooms.datashare.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.datashare.dto.FileDownloadResponseDTO;
import com.openclassrooms.datashare.dto.FileDownloadRequestDTO;
import com.openclassrooms.datashare.dto.FileUploadResponseDTO;
import com.openclassrooms.datashare.dto.UserFileDTO;
import com.openclassrooms.datashare.entities.User;
import com.openclassrooms.datashare.repository.FileRepository;
import com.openclassrooms.datashare.repository.UserRepository;
import com.openclassrooms.datashare.service.FileService;
import com.openclassrooms.datashare.service.SupabaseStorageService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {
    @Mock FileService fileService;
    private FileController controller;
    private User user;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        controller = new FileController(fileService);
        user = new User();
        user.setId(42L);
        user.setLogin("user@example.com");
        authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Test
    void listsFilesForTheAuthenticatedUser() {
        UserFileDTO file = new UserFileDTO(1L, "file.txt", 2L, "text/plain", "path",
                Instant.now().plusSeconds(60), false);
        when(fileService.getAllFilesByUser(user.getLogin())).thenReturn(List.of(file));

        var response = controller.getAllFilesByUser(authentication);

        assertThat(response.getBody()).containsExactly(file);
    }

    @Test
    void returnsAnOwnedFileById() {
        UserFileDTO file = new UserFileDTO(1L, "file.txt", 2L, "text/plain", "path",
                Instant.now().plusSeconds(60), false);
        when(fileService.getFileById(1L, user.getLogin())).thenReturn(file);

        var response = controller.getFileById(1L, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(file);
        verify(fileService).getFileById(1L, user.getLogin());
    }

    @Test
    void deletesAnOwnedFile() {
        var response = controller.deleteFile(1L, authentication);
        verify(fileService).deleteFile(1L, user.getLogin());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deletePropagatesTheServiceStatus() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(fileService)
                .deleteFile(99L, user.getLogin());
        assertThat(controller.deleteFile(99L, authentication).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void downloadsAnOwnedFileAsAnAttachment() {
        when(fileService.download("users/42/file", user.getLogin()))
                .thenReturn(new FileService.DownloadResult("report.txt", "text/plain", new byte[] { 1, 2 }));

        var response = controller.download("users/42/file", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/plain");
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("report.txt");
        assertThat(response.getBody()).containsExactly(1, 2);
    }

    @Test
    void downloadPropagatesTheServiceStatus() {
        when(fileService.download(any(), any())).thenThrow(new ResponseStatusException(HttpStatus.GONE));
        assertThat(controller.download("path", authentication).getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void uploadAcceptsTheAuthenticatedUsersId() {
        MockMultipartFile multipart = new MockMultipartFile("file", "file.txt", "text/plain", new byte[] { 1 });
        FileUploadResponseDTO dto = new FileUploadResponseDTO("file.txt", 1, "text/plain", "stored",
                1L, 42L, "files", "path", false, "token");
        when(fileService.upload(multipart, user.getLogin(), 7, null)).thenReturn(dto);

        var response = controller.upload(multipart, 42L, 7, null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void uploadRejectsAnotherUsersIdBeforeCallingTheService() {
        MockMultipartFile multipart = new MockMultipartFile("file", new byte[] { 1 });
        assertThat(controller.upload(multipart, 99L, 7, null, authentication).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(fileService);
    }

    @Test
    void tokenDownloadDoesNotRequireAuthentication() {
        when(fileService.downloadWithToken("token", "secret"))
                .thenReturn(new FileService.DownloadResult("file.bin", "invalid content type", new byte[] { 3 }));
        var response = controller.downloadWithToken(new FileDownloadRequestDTO("token", "secret"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/octet-stream");
    }

    @Test
    void tokenDownloadPropagatesTheServiceStatus() {
        when(fileService.downloadWithToken("token", null))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertThat(controller.downloadWithToken(new FileDownloadRequestDTO("token", null)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void returnsFileInformationByToken() {
        FileDownloadResponseDTO dto = new FileDownloadResponseDTO("file.txt", 1L, "text/plain", "stored",
                1L, 42L, "files", "path", false, Instant.now());
        when(fileService.getFileDetailByToken("token")).thenReturn(dto);
        assertThat(controller.getFileInfoByToken("token").getBody()).isEqualTo(dto);
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class IntegrationTests extends PostgreSQLIntegrationTest {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @Autowired UserRepository integrationUserRepository;
        @Autowired FileRepository integrationFileRepository;
        @Autowired PasswordEncoder integrationPasswordEncoder;

        @MockitoBean
        SupabaseStorageService integrationStorageService;

        private User integrationUser;
        private String authorization;

        @BeforeEach
        void setUpIntegrationTest() throws Exception {
            integrationUser = new User();
            integrationUser.setLogin("file-owner@example.com");
            integrationUser.setPassword(integrationPasswordEncoder.encode("password123"));
            integrationUser = integrationUserRepository.saveAndFlush(integrationUser);
            when(integrationStorageService.getBucket()).thenReturn("documents");
            authorization = "Bearer " + login(integrationUser.getLogin(), "password123");
        }

        @Test
        void integrationTestUploadsListsAndDeletesAnOwnedFile() throws Exception {
            MockMultipartFile multipartFile = new MockMultipartFile(
                    "file", "report.txt", "text/plain", "integration content".getBytes());

            String uploadBody = mockMvc.perform(multipart("/api/file")
                            .file(multipartFile)
                            .param("userId", integrationUser.getId().toString())
                            .param("expirationDays", "3")
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.filename").value("report.txt"))
                    .andExpect(jsonPath("$.status").value("stored"))
                    .andExpect(jsonPath("$.userId").value(integrationUser.getId()))
                    .andReturn().getResponse().getContentAsString();

            JsonNode upload = objectMapper.readTree(uploadBody);
            long fileId = upload.get("id").asLong();
            String objectPath = upload.get("objectPath").asText();
            assertThat(integrationFileRepository.findById(fileId)).isPresent();
            verify(integrationStorageService).upload(eq(objectPath), any(), eq("text/plain"));

            mockMvc.perform(get("/api/files").header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(fileId))
                    .andExpect(jsonPath("$[0].filename").value("report.txt"));

            mockMvc.perform(get("/api/file/{fileId}", fileId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(fileId))
                    .andExpect(jsonPath("$.filename").value("report.txt"))
                    .andExpect(jsonPath("$.objectPath").value(objectPath));

            mockMvc.perform(delete("/api/file/{fileId}", fileId)
                            .header("Authorization", authorization))
                    .andExpect(status().isNoContent());

            verify(integrationStorageService).delete(objectPath);
            assertThat(integrationFileRepository.findById(fileId)).isEmpty();
        }

        @Test
        void integrationTestRejectsFileOperationsWithoutAuthentication() throws Exception {
            MockMultipartFile multipartFile = new MockMultipartFile(
                    "file", "report.txt", "text/plain", new byte[] { 1 });

            mockMvc.perform(multipart("/api/file").file(multipartFile))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/files"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/file/{fileId}", 1L))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(delete("/api/file/{fileId}", 1L))
                    .andExpect(status().isUnauthorized());
        }

        private String login(String login, String password) throws Exception {
            return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/api/login")
                            .contentType("application/json")
                            .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
        }
    }
}
