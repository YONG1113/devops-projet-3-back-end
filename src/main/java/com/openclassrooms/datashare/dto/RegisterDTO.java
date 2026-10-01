package com.openclassrooms.datashare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank
    private String login;
    @NotBlank
    private String password;

}
