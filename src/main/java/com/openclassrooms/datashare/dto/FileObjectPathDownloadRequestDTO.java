package com.openclassrooms.datashare.dto;

import jakarta.validation.constraints.NotBlank;

public record FileObjectPathDownloadRequestDTO(
        @NotBlank(message = "Object path is required") String objectPath,
        String password) {
}
