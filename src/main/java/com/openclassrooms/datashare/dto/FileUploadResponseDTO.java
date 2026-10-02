package com.openclassrooms.datashare.dto;

public record FileUploadResponseDTO(
        String filename,
        long size,
        String contentType,
        String status) {
}
