package com.openclassrooms.datashare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {
    @NotBlank
    private String login;
    @NotBlank
    private String password;

}
