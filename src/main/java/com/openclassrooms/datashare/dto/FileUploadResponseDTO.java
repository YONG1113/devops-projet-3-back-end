package com.openclassrooms.datashare.dto;

public record FileUploadResponseDTO(
                String filename,
                long size,
                String contentType,
                String status,
                Long id,
                Long userId,
                String bucket,
                String objectPath,
                boolean isProtectPassword) {
}
