package com.openclassrooms.datashare.controller;

import com.openclassrooms.datashare.dto.FileUploadResponseDTO;
import com.openclassrooms.datashare.dto.UserFileDTO;
import com.openclassrooms.datashare.service.FileService;
import com.openclassrooms.datashare.entities.User;
import lombok.RequiredArgsConstructor;
import com.openclassrooms.datashare.dto.FileDownloadResponseDTO;
import com.openclassrooms.datashare.dto.FileDownloadRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Fichiers", description = "Envoi, consultation, téléchargement et suppression des fichiers")
public class FileController {

    private final FileService fileService;

    @Operation(summary = "Lister les fichiers de l'utilisateur connecté")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/files")
    public ResponseEntity<List<UserFileDTO>> getAllFilesByUser(
            @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(fileService.getAllFilesByUser(authentication.getName()));
    }

    @Operation(summary = "Supprimer un fichier appartenant à l'utilisateur connecté")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/api/file/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "Identifiant du fichier", required = true) @PathVariable Long fileId,
            @Parameter(hidden = true) Authentication authentication) {
        try {
            fileService.deleteFile(fileId, authentication.getName());
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).build();
        }
    }

    @Operation(summary = "Télécharger un fichier par son chemin de stockage",
            description = "Télécharge un fichier appartenant à l'utilisateur connecté.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/file")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "Chemin de l'objet dans le bucket", required = true)
            @RequestParam("objectPath") String objectPath,
            @Parameter(hidden = true) Authentication authentication) {
        try {
            var file = fileService.download(objectPath, authentication.getName());
            String filename = file.filename().replaceAll("[\\r\\n]", "_");
            return ResponseEntity.ok()
                    .contentType(resolveMediaType(file.contentType()))
                    .contentLength(file.content().length)
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .body(file.content());
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).build();
        }
    }

    private MediaType resolveMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @Operation(summary = "Téléverser un fichier",
            description = "Conserve un fichier pendant 1 à 7 jours avec un mot de passe facultatif.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/api/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponseDTO> upload(
            @Parameter(description = "Fichier à téléverser", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Identifiant facultatif de l'utilisateur connecté")
            @RequestParam(value = "userId", required = false) Long userId,
            @Parameter(description = "Durée de conservation en jours, entre 1 et 7")
            @RequestParam(value = "expirationDays", defaultValue = "7") int expirationDays,
            @Parameter(description = "Mot de passe de téléchargement facultatif")
            @RequestParam(value = "password", required = false) String password,
            @Parameter(hidden = true) Authentication authentication) {
        if (userId != null && (!(authentication.getPrincipal() instanceof User user)
                || !userId.equals(user.getId()))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(fileService.upload(file, authentication.getName(), expirationDays, password));
    }

    @Operation(summary = "Télécharger un fichier avec un jeton public",
            description = "Reçoit le jeton et le mot de passe facultatif dans un corps JSON afin de ne pas exposer le mot de passe dans l'URL.")
    @PostMapping(value = "/api/file/download", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> downloadWithToken(
            @Valid @RequestBody FileDownloadRequestDTO request) {
        try {
            var file = fileService.downloadWithToken(request.token(), request.password());
            String filename = file.filename().replaceAll("[\\r\\n]", "_");
            return ResponseEntity.ok()
                    .contentType(resolveMediaType(file.contentType()))
                    .contentLength(file.content().length)
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .body(file.content());
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).build();
        }
    }

    @Operation(summary = "Consulter les informations d'un fichier avec un jeton public")
    @GetMapping("/api/file/info")
    public ResponseEntity<FileDownloadResponseDTO> getFileInfoByToken(
            @Parameter(description = "Jeton public de téléchargement", required = true)
            @RequestParam("token") String token) {
        return ResponseEntity.ok(fileService.getFileDetailByToken(token));
    }

}
