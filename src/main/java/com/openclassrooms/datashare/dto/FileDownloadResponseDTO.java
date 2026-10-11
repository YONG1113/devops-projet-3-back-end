package com.openclassrooms.datashare.dto;

import java.time.Instant;

public record FileDownloadResponseDTO(String filename,
        Long size,
        String contentType,
        String status,
        Long id,
        Long userId,
        String bucket,
        String objectPath,
        Boolean isProtectPassword,
        String downloadToken,
        Instant expiresAt) {
}
