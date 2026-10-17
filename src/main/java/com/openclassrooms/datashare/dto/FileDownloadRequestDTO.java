package com.openclassrooms.datashare.dto;

import jakarta.validation.constraints.NotBlank;

public record FileDownloadRequestDTO(
        @NotBlank(message = "Download token is required") String token,
        String password) {
}
