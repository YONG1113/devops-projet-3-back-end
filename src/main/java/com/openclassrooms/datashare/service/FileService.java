package com.openclassrooms.datashare.service;

import com.openclassrooms.datashare.dto.FileUploadResponseDTO;
import com.openclassrooms.datashare.dto.UserFileDTO;
import com.openclassrooms.datashare.entities.File;
import com.openclassrooms.datashare.repository.FileRepository;
import com.openclassrooms.datashare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final SupabaseStorageService storageService;
    private final PasswordEncoder passwordEncoder;
    private final DownloadTokenService downloadTokenService;

    public record DownloadResult(String filename, String contentType, byte[] content) {
    }

    public List<UserFileDTO> getAllFilesByUser(String login) {
        Assert.hasText(login, "Authenticated user is required");
        userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return fileRepository.findAllByUserLoginOrderByCreatedAtDesc(login)
                .stream()
                .map(record -> new UserFileDTO(
                        record.getId(),
                        record.getOriginalName(),
                        record.getSize(),
                        record.getContentType(),
                        record.getObjectPath(),
                        record.getExpiresAt(),
                        record.getPasswordHash() != null && !record.getPasswordHash().isBlank(),
                        record.getDownloadTokenHash()))
                .toList();
    }

    public DownloadResult download(String objectPath, String login) {
        Assert.hasText(objectPath, "Object path is required");
        Assert.hasText(login, "Authenticated user is required");
        String bucket = storageService.getBucket();
        File record = fileRepository.findByBucketAndObjectPathAndUserLogin(bucket, objectPath, login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (record.getExpiresAt() == null || !record.getExpiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "File has expired");
        }

        byte[] content = storageService.download(record.getObjectPath());
        String contentType = record.getContentType() == null || record.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : record.getContentType();
        return new DownloadResult(record.getOriginalName(), contentType, content);
    }

    public DownloadResult downloadWithToken(String token, String password) {
        Assert.hasText(token, "token is required");
        File record = fileRepository.findByDownloadTokenHash(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        String bucket = storageService.getBucket();

        if (record.getExpiresAt() == null || !record.getExpiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "File has expired");
        }

        if (record.getPasswordHash() != null) {
            if (password == null ||
                    !passwordEncoder.matches(password, record.getPasswordHash())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Invalid download password");
            }
        }

        byte[] content = storageService.download(record.getObjectPath());
        String contentType = record.getContentType() == null || record.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : record.getContentType();
        return new DownloadResult(record.getOriginalName(), contentType, content);
    }

    public FileUploadResponseDTO getFileDetailByToken(String token) {
        Assert.hasText(token, "token is required");
        File record = fileRepository.findByDownloadTokenHash(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        String bucket = storageService.getBucket();

        if (record.getExpiresAt() == null || !record.getExpiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "File has expired");
        }

        return new FileUploadResponseDTO(record.getOriginalName(), record.getSize(),
                record.getContentType(), "stored", record.getId(), record.getUser().getId(),
                record.getBucket(), record.getObjectPath(),
                record.getPasswordHash() != null && !record.getPasswordHash().isBlank(), record.getDownloadTokenHash());

    }

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

        String downloadToken = downloadTokenService.generateToken();
        String tokenHash = downloadTokenService.hashToken(downloadToken);
        record.setDownloadTokenHash(tokenHash);

        record = fileRepository.saveAndFlush(record);

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
                record.getBucket(), record.getObjectPath(),
                record.getPasswordHash() != null && !record.getPasswordHash().isBlank(), record.getDownloadTokenHash());
    }

}
