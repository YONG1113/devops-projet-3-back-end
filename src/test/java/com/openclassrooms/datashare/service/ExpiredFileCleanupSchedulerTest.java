package com.openclassrooms.datashare.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExpiredFileCleanupSchedulerTest {
    @Test
    void scheduledExecutionDelegatesToFileService() {
        FileService fileService = mock(FileService.class);
        ExpiredFileCleanupScheduler scheduler = new ExpiredFileCleanupScheduler(fileService);

        scheduler.cleanupExpiredFiles();

        verify(fileService).cleanupExpiredFiles();
    }
}
