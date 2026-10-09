package com.openclassrooms.datashare.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredFileCleanupScheduler {

    private final FileService fileService;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredFiles() {
        log.info("Starting expired file cleanup");
        fileService.cleanupExpiredFiles();
    }
}
