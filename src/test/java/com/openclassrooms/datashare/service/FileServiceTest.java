package com.openclassrooms.datashare.service;

import com.openclassrooms.datashare.entities.File;
import com.openclassrooms.datashare.entities.User;
import com.openclassrooms.datashare.repository.FileRepository;
import com.openclassrooms.datashare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @Mock UserRepository users;
    @Mock FileRepository files;
    @Mock SupabaseStorageService storage;
    @Mock PasswordEncoder passwordEncoder;
    @Mock DownloadTokenService tokenService;
    private FileService service;
    private User user;
    private MockMultipartFile multipart;

    @BeforeEach
    void setUp() {
        service = new FileService(users, files, storage, passwordEncoder, tokenService);
        user = new User();
        user.setId(42L);
        user.setLogin("user@example.com");
        multipart = new MockMultipartFile("file", "hello.txt", "text/plain", new byte[] { 1, 2 });
    }

    @Test
    void uploadStoresMetadataAndTheStorageObject() {
        prepareUpload();
        when(passwordEncoder.encode("secret")).thenReturn("encoded-password");
        when(files.saveAndFlush(any(File.class))).thenAnswer(invocation -> {
            File record = invocation.getArgument(0);
            record.setId(10L);
            return record;
        });

        var result = service.upload(multipart, user.getLogin(), 3, "secret");

        ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
        verify(files).saveAndFlush(captor.capture());
        File record = captor.getValue();
        verify(storage).upload(eq(record.getObjectPath()), same(multipart), eq("text/plain"));
        assertThat(record.getUser()).isSameAs(user);
        assertThat(record.getObjectPath()).startsWith("users/42/");
        assertThat(record.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(record.getDownloadTokenHash()).isEqualTo("hashed-token");
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo("stored");
        assertThat(result.downloadToken()).isEqualTo("raw-token");
    }

    @Test
    void uploadUsesBinaryContentTypeAndAllowsAnEmptyPassword() {
        prepareUpload();
        when(files.saveAndFlush(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile binary = new MockMultipartFile("file", "binary", null, new byte[] { 1 });

        var result = service.upload(binary, user.getLogin(), 7, "");

        assertThat(result.contentType()).isEqualTo("application/octet-stream");
        assertThat(result.isProtectPassword()).isFalse();
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void uploadRejectsInvalidFilesBeforeStorageAccess() {
        MultipartFile oversized = mock(MultipartFile.class);
        when(oversized.isEmpty()).thenReturn(false);
        when(oversized.getOriginalFilename()).thenReturn("large.zip");
        when(oversized.getSize()).thenReturn(1024L * 1024 * 1024 + 1);

        assertStatus(() -> service.upload(oversized, user.getLogin(), 7, null), HttpStatus.PAYLOAD_TOO_LARGE);
        assertStatus(() -> service.upload(
                new MockMultipartFile("file", "virus.EXE", "application/octet-stream", new byte[] { 1 }),
                user.getLogin(), 7, null), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", new byte[0]), user.getLogin(), 7, null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(storage, files);
    }

    @Test
    void uploadRejectsInvalidExpirationAndUnknownUser() {
        assertThatThrownBy(() -> service.upload(multipart, user.getLogin(), 8, null))
                .isInstanceOf(IllegalArgumentException.class);
        when(users.findByLogin(user.getLogin())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.upload(multipart, user.getLogin(), 7, null))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void getAllFilesMapsRepositoryRecords() {
        File record = record(Instant.now().plusSeconds(60));
        record.setPasswordHash("hash");
        when(users.findByLogin(user.getLogin())).thenReturn(Optional.of(user));
        when(files.findAllByUserLoginOrderByCreatedAtAsc(user.getLogin())).thenReturn(List.of(record));

        var result = service.getAllFilesByUser(user.getLogin());

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.filename()).isEqualTo("hello.txt");
            assertThat(dto.isProtectPassword()).isTrue();
        });
    }

    @Test
    void getAllFilesMarksAFileWithoutPasswordAsUnprotected() {
        File record = record(Instant.now().plusSeconds(60));
        record.setPasswordHash(null);
        when(users.findByLogin(user.getLogin())).thenReturn(Optional.of(user));
        when(files.findAllByUserLoginOrderByCreatedAtAsc(user.getLogin())).thenReturn(List.of(record));

        var result = service.getAllFilesByUser(user.getLogin());

        assertThat(result).singleElement()
                .extracting(dto -> dto.isProtectPassword())
                .isEqualTo(false);
    }

    @Test
    void getAllFilesRejectsAnUnknownUser() {
        when(users.findByLogin("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAllFilesByUser("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(files);
    }

    @Test
    void getFileByIdReturnsAnOwnedFile() {
        File record = record(Instant.now().plusSeconds(60));
        record.setPasswordHash("hash");
        when(files.findByIdAndUserLogin(1L, user.getLogin())).thenReturn(Optional.of(record));

        var result = service.getFileById(1L, user.getLogin());

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.filename()).isEqualTo("hello.txt");
        assertThat(result.objectPath()).isEqualTo("users/42/object");
        assertThat(result.isProtectPassword()).isTrue();
    }

    @Test
    void getFileByIdRejectsAnUnknownOrUnownedFile() {
        when(files.findByIdAndUserLogin(99L, user.getLogin())).thenReturn(Optional.empty());

        assertStatus(() -> service.getFileById(99L, user.getLogin()), HttpStatus.NOT_FOUND);
    }

    @Test
    void getFileByIdRejectsANonPositiveIdBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.getFileById(0L, user.getLogin()))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(files);
    }

    @Test
    void deleteRemovesStorageBeforeMetadata() {
        File record = record(Instant.now().plusSeconds(60));
        when(files.findByIdAndUserLogin(1L, user.getLogin())).thenReturn(Optional.of(record));

        service.deleteFile(1L, user.getLogin());

        var order = inOrder(storage, files);
        order.verify(storage).delete("users/42/object");
        order.verify(files).delete(record);
    }

    @Test
    void deleteRejectsAnUnknownFile() {
        when(files.findByIdAndUserLogin(99L, user.getLogin())).thenReturn(Optional.empty());
        assertStatus(() -> service.deleteFile(99L, user.getLogin()), HttpStatus.NOT_FOUND);
        verifyNoInteractions(storage);
    }

    @Test
    void deleteRejectsANonPositiveFileIdBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.deleteFile(0L, user.getLogin()))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(files, storage);
    }

    @Test
    void cleanupClearsStorageMetadataButPreservesHistoryFields() {
        File record = record(Instant.now().minusSeconds(60));
        record.setPasswordHash("hash");
        record.setCreatedAt(Instant.now().minusSeconds(120));
        when(files.findByExpiresAtLessThanEqualOrderByExpiresAtAsc(any())).thenReturn(List.of(record));

        service.cleanupExpiredFiles();

        verify(storage).delete("users/42/object");
        verify(files).saveAndFlush(record);
        assertThat(record.getId()).isEqualTo(1L);
        assertThat(record.getUser()).isSameAs(user);
        assertThat(record.getOriginalName()).isEqualTo("hello.txt");
        assertThat(record.getExpiresAt()).isNotNull();
        assertThat(record.getDownloadTokenHash()).isEqualTo("hashed-token");
        assertThat(record.getBucket()).isNull();
        assertThat(record.getObjectPath()).isNull();
        assertThat(record.getSize()).isNull();
        assertThat(record.getPasswordHash()).isNull();
    }

    @Test
    void cleanupContinuesWhenStorageDeletionFails() {
        File record = record(Instant.now().minusSeconds(60));
        when(files.findByExpiresAtLessThanEqualOrderByExpiresAtAsc(any())).thenReturn(List.of(record));
        doThrow(new StorageException("failed")).when(storage).delete(record.getObjectPath());

        service.cleanupExpiredFiles();

        verify(files, never()).saveAndFlush(any());
        assertThat(record.getObjectPath()).isEqualTo("users/42/object");
    }

    @Test
    void downloadReturnsStreamingMetadataForAnOwnedActiveFile() {
        File record = record(Instant.now().plusSeconds(60));
        when(storage.getBucket()).thenReturn("documents");
        when(files.findByBucketAndObjectPathAndUserLogin("documents", record.getObjectPath(), user.getLogin()))
                .thenReturn(Optional.of(record));
        var result = service.download(record.getObjectPath(), user.getLogin(), null);

        assertThat(result.filename()).isEqualTo("hello.txt");
        assertThat(result.objectPath()).isEqualTo(record.getObjectPath());
        assertThat(result.contentLength()).isEqualTo(record.getSize());
    }

    @Test
    void downloadChecksTheObjectPathPassword() {
        File record = record(Instant.now().plusSeconds(60));
        record.setPasswordHash("password-hash");
        when(storage.getBucket()).thenReturn("documents");
        when(files.findByBucketAndObjectPathAndUserLogin("documents", record.getObjectPath(), user.getLogin()))
                .thenReturn(Optional.of(record));
        when(passwordEncoder.matches("secret", "password-hash")).thenReturn(true);
        assertThat(service.download(record.getObjectPath(), user.getLogin(), "secret").objectPath())
                .isEqualTo(record.getObjectPath());
        assertStatus(
                () -> service.download(record.getObjectPath(), user.getLogin(), "wrong"),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void downloadRejectsExpiredAndUnknownFiles() {
        when(storage.getBucket()).thenReturn("documents");
        when(files.findByBucketAndObjectPathAndUserLogin("documents", "missing", user.getLogin()))
                .thenReturn(Optional.empty());
        assertStatus(() -> service.download("missing", user.getLogin(), null), HttpStatus.NOT_FOUND);

        File expired = record(Instant.now().minusSeconds(1));
        when(files.findByBucketAndObjectPathAndUserLogin("documents", expired.getObjectPath(), user.getLogin()))
                .thenReturn(Optional.of(expired));
        assertStatus(() -> service.download(expired.getObjectPath(), user.getLogin(), null), HttpStatus.GONE);
    }

    @Test
    void downloadRejectsAFileWithoutExpiration() {
        File record = record(null);
        when(storage.getBucket()).thenReturn("documents");
        when(files.findByBucketAndObjectPathAndUserLogin("documents", record.getObjectPath(), user.getLogin()))
                .thenReturn(Optional.of(record));

        assertStatus(() -> service.download(record.getObjectPath(), user.getLogin(), null), HttpStatus.GONE);
        verify(storage, never()).downloadTo(anyString(), any());
    }

    @Test
    void downloadUsesTheBinaryContentTypeWhenMetadataIsBlank() {
        File record = record(Instant.now().plusSeconds(60));
        record.setContentType(" ");
        when(storage.getBucket()).thenReturn("documents");
        when(files.findByBucketAndObjectPathAndUserLogin("documents", record.getObjectPath(), user.getLogin()))
                .thenReturn(Optional.of(record));
        var result = service.download(record.getObjectPath(), user.getLogin(), null);

        assertThat(result.contentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void tokenDownloadChecksTheOptionalPassword() {
        File record = record(Instant.now().plusSeconds(60));
        record.setPasswordHash("password-hash");
        when(tokenService.hashToken("token")).thenReturn("hashed-token");
        when(files.findByDownloadTokenHash("hashed-token")).thenReturn(Optional.of(record));
        when(passwordEncoder.matches("secret", "password-hash")).thenReturn(true);

        var result = service.downloadWithToken("token", "secret");
        assertThat(result.objectPath()).isEqualTo(record.getObjectPath());
        assertThat(result.contentLength()).isEqualTo(record.getSize());
        assertStatus(() -> service.downloadWithToken("token", null), HttpStatus.FORBIDDEN);
    }

    @Test
    void tokenDownloadAllowsAnUnprotectedFileAndUsesTheBinaryContentType() {
        File record = record(Instant.now().plusSeconds(60));
        record.setPasswordHash(null);
        record.setContentType(null);
        when(tokenService.hashToken("token")).thenReturn("hashed-token");
        when(files.findByDownloadTokenHash("hashed-token")).thenReturn(Optional.of(record));

        var result = service.downloadWithToken("token", null);

        assertThat(result.contentType()).isEqualTo("application/octet-stream");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void tokenDownloadRejectsAnExpiredFileAndAnIncorrectPassword() {
        File expired = record(Instant.now().minusSeconds(1));
        File protectedRecord = record(Instant.now().plusSeconds(60));
        protectedRecord.setPasswordHash("password-hash");
        when(tokenService.hashToken("expired")).thenReturn("hashed-expired");
        when(tokenService.hashToken("protected")).thenReturn("hashed-protected");
        when(files.findByDownloadTokenHash("hashed-expired")).thenReturn(Optional.of(expired));
        when(files.findByDownloadTokenHash("hashed-protected")).thenReturn(Optional.of(protectedRecord));
        when(passwordEncoder.matches("wrong", "password-hash")).thenReturn(false);

        assertStatus(() -> service.downloadWithToken("expired", null), HttpStatus.GONE);
        assertStatus(() -> service.downloadWithToken("protected", "wrong"), HttpStatus.FORBIDDEN);
        verify(storage, never()).downloadTo(anyString(), any());
    }

    @Test
    void fileDetailsReturnArchivedInformationForAnExpiredFile() {
        File record = record(Instant.now().minusSeconds(1));
        when(tokenService.hashToken("token")).thenReturn("hashed-token");
        when(files.findByDownloadTokenHash("hashed-token")).thenReturn(Optional.of(record));

        var result = service.getFileDetailByToken("token");

        assertThat(result.status()).isEqualTo("expired");
        assertThat(result.filename()).isEqualTo("hello.txt");
        assertThat(result.size()).isNull();
        assertThat(result.objectPath()).isNull();
    }

    @Test
    void fileDetailsReturnActiveInformationAndPasswordProtection() {
        File record = record(Instant.now().plusSeconds(60));
        record.setPasswordHash("password-hash");
        when(tokenService.hashToken("token")).thenReturn("hashed-token");
        when(files.findByDownloadTokenHash("hashed-token")).thenReturn(Optional.of(record));

        var result = service.getFileDetailByToken("token");

        assertThat(result.status()).isEqualTo("stored");
        assertThat(result.objectPath()).isEqualTo("users/42/object");
        assertThat(result.isProtectPassword()).isTrue();
    }

    @Test
    void fileDetailsTreatAFileWithoutExpirationAsExpired() {
        File record = record(null);
        when(tokenService.hashToken("token")).thenReturn("hashed-token");
        when(files.findByDownloadTokenHash("hashed-token")).thenReturn(Optional.of(record));

        var result = service.getFileDetailByToken("token");

        assertThat(result.status()).isEqualTo("expired");
        assertThat(result.expiresAt()).isNull();
    }

    @Test
    void uploadRejectsAnOverlongFilenameAndContentType() {
        String longFilename = "a".repeat(252) + ".txt";
        MockMultipartFile invalidName = new MockMultipartFile(
                "file", longFilename, "text/plain", new byte[] { 1 });
        MockMultipartFile invalidContentType = new MockMultipartFile(
                "file", "hello.txt", "x".repeat(256), new byte[] { 1 });

        assertThatThrownBy(() -> service.upload(invalidName, user.getLogin(), 7, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.upload(invalidContentType, user.getLogin(), 7, null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(files, storage);
    }

    private void prepareUpload() {
        when(users.findByLogin(user.getLogin())).thenReturn(Optional.of(user));
        when(storage.getBucket()).thenReturn("documents");
        when(tokenService.generateToken()).thenReturn("raw-token");
        when(tokenService.hashToken("raw-token")).thenReturn("hashed-token");
    }

    private File record(Instant expiresAt) {
        File record = new File();
        record.setId(1L);
        record.setUser(user);
        record.setOriginalName("hello.txt");
        record.setBucket("documents");
        record.setObjectPath("users/42/object");
        record.setSize(2L);
        record.setContentType("text/plain");
        record.setExpiresAt(expiresAt);
        record.setDownloadTokenHash("hashed-token");
        return record;
    }

    private void assertStatus(ThrowingCallable callable, HttpStatus status) {
        assertThatThrownBy(callable).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
    }

    @FunctionalInterface
    private interface ThrowingCallable extends org.assertj.core.api.ThrowableAssert.ThrowingCallable {
    }
}
