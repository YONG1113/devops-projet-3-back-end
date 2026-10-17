package com.openclassrooms.datashare.controller;

import com.openclassrooms.datashare.dto.LoginRequestDTO;
import com.openclassrooms.datashare.dto.RegisterDTO;
import com.openclassrooms.datashare.mapper.UserDtoMapper;
import com.openclassrooms.datashare.service.UserService;
import com.openclassrooms.datashare.entities.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Inscription, authentification et utilisateur connecté")
public class UserController {

    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    @Operation(summary = "Obtenir l'utilisateur connecté",
            description = "Retourne l'identifiant de l'utilisateur authentifié.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/me")
    public Map<String, String> currentUser(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return Map.of("id", user.getId().toString());
    }

    @Operation(summary = "Créer un compte utilisateur")
    @PostMapping("/api/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(userDtoMapper.toEntity(registerDTO));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Operation(summary = "Se connecter",
            description = "Vérifie les identifiants et retourne un jeton JWT.")
    @PostMapping("/api/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        String jwtToken = userService.login(loginRequestDTO.getLogin(), loginRequestDTO.getPassword());
        return ResponseEntity.ok(jwtToken);
    }

}
