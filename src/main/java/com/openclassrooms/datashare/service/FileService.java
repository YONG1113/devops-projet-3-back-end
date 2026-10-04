package com.openclassrooms.datashare.service;

import com.openclassrooms.datashare.dto.FileUploadResponseDTO;
import com.openclassrooms.datashare.entities.File;
import com.openclassrooms.datashare.repository.FileRepository;
import com.openclassrooms.datashare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final SupabaseStorageService storageService;
    private final PasswordEncoder passwordEncoder;

    public FileUploadResponseDTO upload(MultipartFile file, String login, int expirationDays, String password) {
        Assert.isTrue(expirationDays >= 1 && expirationDays <= 7,
                "Expiration must be between 1 and 7 days");
        Assert.hasText(login, "Authenticated user is required");
        Assert.notNull(file, "File is required");
        Assert.isTrue(!file.isEmpty(), "File must not be empty");
        String filename = file.getOriginalFilename();
        Assert.hasText(filename, "Filename is required");
        Assert.isTrue(filename.length() <= 255, "Filename is too long");
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        Assert.isTrue(contentType.length() <= 255, "Content type is too long");
        MediaType.parseMediaType(contentType);

        var user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String bucket = storageService.getBucket();
        String path = "users/" + user.getId() + "/" + UUID.randomUUID();
        File record = new File();
        record.setUser(user);
        record.setOriginalName(filename);
        record.setBucket(bucket);
        record.setObjectPath(path);
        record.setSize(file.getSize());
        record.setContentType(contentType);

        record.setExpiresAt(
                Instant.now().plus(expirationDays, ChronoUnit.DAYS));

        record.setPasswordHash(
                password == null || password.isEmpty()
                        ? null
                        : passwordEncoder.encode(password));

        storageService.upload(path, file, contentType);
        try {
            record = fileRepository.saveAndFlush(record);
        } catch (RuntimeException databaseFailure) {
            try {
                storageService.delete(path);
            } catch (RuntimeException cleanupFailure) {
                log.error("Storage cleanup required for bucket {} object {}", bucket, path);
                databaseFailure.addSuppressed(cleanupFailure);
            }
            throw databaseFailure;
        }
        return new FileUploadResponseDTO(record.getOriginalName(), record.getSize(),
                record.getContentType(), "stored", record.getId(), user.getId(),
                record.getBucket(), record.getObjectPath());
    }
}
