package com.openclassrooms.datashare.dto;

import java.time.Instant;

public record UserFileDTO(
        Long id,
        String filename,
        Long size,
        String contentType,
        String objectPath,
        Instant expiresAt,
        boolean isProtectPassword,
        String downloadToken) {
}
