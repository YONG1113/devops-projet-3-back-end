package com.openclassrooms.datashare.controller;

import com.openclassrooms.datashare.dto.FileDownloadResponseDTO;
import com.openclassrooms.datashare.dto.FileUploadResponseDTO;
import com.openclassrooms.datashare.dto.UserFileDTO;
import com.openclassrooms.datashare.entities.User;
import com.openclassrooms.datashare.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
                Instant.now().plusSeconds(60), false, "token");
        when(fileService.getAllFilesByUser(user.getLogin())).thenReturn(List.of(file));

        var response = controller.getAllFilesByUser(authentication);

        assertThat(response.getBody()).containsExactly(file);
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
        var response = controller.downloadWithToken("token", "secret");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/octet-stream");
    }

    @Test
    void tokenDownloadPropagatesTheServiceStatus() {
        when(fileService.downloadWithToken("token", null))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertThat(controller.downloadWithToken("token", null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void returnsFileInformationByToken() {
        FileDownloadResponseDTO dto = new FileDownloadResponseDTO("file.txt", 1L, "text/plain", "stored",
                1L, 42L, "files", "path", false, "token", Instant.now());
        when(fileService.getFileDetailByToken("token")).thenReturn(dto);
        assertThat(controller.getFileInfoByToken("token").getBody()).isEqualTo(dto);
    }
}
