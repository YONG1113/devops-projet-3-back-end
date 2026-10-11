package com.openclassrooms.datashare.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DownloadTokenServiceTest {
    private final DownloadTokenService service = new DownloadTokenService();

    @Test
    void generatedTokensAreUrlSafeAndRandom() {
        String first = service.generateToken();
        String second = service.generateToken();

        assertThat(first).matches("^[A-Za-z0-9_-]{43}$");
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void hashingIsStableAndDoesNotExposeTheRawToken() {
        String hash = service.hashToken("download-token");

        assertThat(hash).hasSize(64).matches("^[0-9a-f]+$");
        assertThat(service.hashToken("download-token")).isEqualTo(hash);
        assertThat(hash).doesNotContain("download-token");
    }
}
