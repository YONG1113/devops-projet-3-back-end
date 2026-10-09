package com.openclassrooms.datashare.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "data_file", uniqueConstraints = @UniqueConstraint(name = "uk_data_file_object", columnNames = { "bucket",
        "object_path" }))
@Getter
@Setter
@NoArgsConstructor
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(columnDefinition = "text")
    private String bucket;

    @Column(name = "object_path", columnDefinition = "text")
    private String objectPath;

    @Column
    private Long size;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "download_token_hash", nullable = false, unique = true, length = 64)
    private String downloadTokenHash;
}
