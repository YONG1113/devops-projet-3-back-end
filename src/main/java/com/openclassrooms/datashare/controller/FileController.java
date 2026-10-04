package com.openclassrooms.datashare.controller;

import com.openclassrooms.datashare.dto.FileUploadResponseDTO;
import com.openclassrooms.datashare.service.FileService;
import com.openclassrooms.datashare.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/api/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponseDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "expirationDays", defaultValue = "7") int expirationDays,
            @RequestParam(value = "password", required = false) String password,
            Authentication authentication) {
        if (userId != null && (!(authentication.getPrincipal() instanceof User user)
                || !userId.equals(user.getId()))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(fileService.upload(file, authentication.getName(), expirationDays, password));
    }

}
