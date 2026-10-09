package com.openclassrooms.datashare.dto;

import java.time.Instant;

public record FileDownloadResponseDTO(String filename,
        long size,
        String contentType,
        String status,
        Long id,
        Long userId,
        String bucket,
        String objectPath,
        boolean isProtectPassword,
        String downloadToken,
        Instant expiresAt) {
}
