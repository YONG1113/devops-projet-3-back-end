package com.openclassrooms.datashare.repository;

import com.openclassrooms.datashare.entities.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findByBucketAndObjectPathAndUserLogin(
            String bucket,
            String objectPath,
            String login);

    Optional<File> findByDownloadTokenHash(String downloadTokenHash);

    List<File> findAllByUserLoginOrderByCreatedAtDesc(String login);

    Optional<File> findByIdAndUserLogin(Long id, String login);
}
