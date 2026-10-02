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
import org.springframework.mock.web.MockMultipartFile;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @Mock
    UserRepository users;
    @Mock
    FileRepository files;
    @Mock
    SupabaseStorageService storage;
    FileService service;
    User user;
    MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", new byte[] { 1, 2 });

    @BeforeEach
    void setup() {
        service = new FileService(users, files, storage);
        user = new User();
        user.setId(42L);
        user.setLogin("user@example.com");
    }

    void prepare() {
        when(users.findByLogin(user.getLogin())).thenReturn(Optional.of(user));
        when(storage.getBucket()).thenReturn("documents");
    }

    // @Test
    // void uploadsBeforeSavingMetadataWithAuthenticatedOwner() {
    // prepare();
    // when(files.saveAndFlush(any())).thenAnswer(call -> {
    // File record = call.getArgument(0);
    // record.setId(10L);
    // return record;
    // });
    // var result = service.upload(file, user.getLogin());
    // var order = inOrder(storage, files);
    // order.verify(storage).getBucket();
    // order.verify(storage).upload(eq(result.objectPath()), same(file),
    // eq("text/plain"));
    // var captor = ArgumentCaptor.forClass(File.class);
    // order.verify(files).saveAndFlush(captor.capture());
    // assertThat(captor.getValue().getUser()).isSameAs(user);
    // assertThat(result.objectPath()).startsWith("users/42/");
    // assertThat(result.id()).isEqualTo(10L);
    // assertThat(result.status()).isEqualTo("stored");
    // assertThat(result.filename()).isEqualTo("hello.txt");
    // assertThat(result.size()).isEqualTo(2);
    // assertThat(result.bucket()).isEqualTo("documents");
    // }

    // @Test
    // void uploadFailureDoesNotWriteDatabase() {
    // prepare();
    // doThrow(new StorageException("upload
    // failed")).when(storage).upload(anyString(), any(), anyString());
    // assertThatThrownBy(() -> service.upload(file,
    // user.getLogin())).isInstanceOf(StorageException.class);
    // verifyNoInteractions(files);
    // verify(storage, never()).delete(anyString());
    // }

    // @Test
    // void databaseFailureRemovesUploadedObject() {
    // prepare();
    // var failure = new RuntimeException("database failed");
    // when(files.saveAndFlush(any())).thenThrow(failure);
    // assertThatThrownBy(() -> service.upload(file,
    // user.getLogin())).isSameAs(failure);
    // var path = ArgumentCaptor.forClass(String.class);
    // verify(storage).upload(path.capture(), same(file), eq("text/plain"));
    // verify(storage).delete(path.getValue());
    // }

    // @Test
    // void cleanupFailurePreservesDatabaseFailure() {
    // prepare();
    // var failure = new RuntimeException("database failed");
    // when(files.saveAndFlush(any())).thenThrow(failure);
    // doThrow(new StorageException("cleanup
    // failed")).when(storage).delete(anyString());
    // assertThatThrownBy(() -> service.upload(file,
    // user.getLogin())).isSameAs(failure);
    // assertThat(failure.getSuppressed()).hasSize(1);
    // }

    // @Test
    // void rejectsEmptyFileAndUnknownUserBeforeStorage() {
    // assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", new
    // byte[0]), "user"))
    // .isInstanceOf(IllegalArgumentException.class);
    // when(users.findByLogin("unknown")).thenReturn(Optional.empty());
    // assertThatThrownBy(() -> service.upload(file, "unknown"))
    // .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    // verifyNoInteractions(storage, files);
    // }

    // @Test
    // void missingContentTypeUsesBinaryDefault() {
    // prepare();
    // when(files.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    // var binary = new MockMultipartFile("file", "binary", null, new byte[] { 1 });
    // assertThat(service.upload(binary,
    // user.getLogin()).contentType()).isEqualTo("application/octet-stream");
    // }
}
