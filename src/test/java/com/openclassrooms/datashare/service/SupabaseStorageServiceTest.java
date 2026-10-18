package com.openclassrooms.datashare.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SupabaseStorageServiceTest {
    private MockRestServiceServer server;
    private SupabaseStorageService storage;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.supabase.co/storage/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        storage = new SupabaseStorageService(builder.build(), "documents");
    }

    @Test
    void uploadsTheResourceToTheObjectEndpoint() {
        server.expect(requestTo("https://example.supabase.co/storage/v1/object/documents/users%2F42%2Funique"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-upsert", "false"))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        storage.upload("users/42/unique",
                new MockMultipartFile("file", "a.txt", "text/plain", new byte[] { 1, 2 }), "text/plain");

        server.verify();
    }

    @Test
    void downloadsFileBytes() {
        server.expect(requestTo("https://example.supabase.co/storage/v1/object/authenticated/documents/users/42/unique"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] { 1, 2 }, MediaType.APPLICATION_OCTET_STREAM));

        assertThat(storage.download("users/42/unique")).containsExactly(1, 2);
        server.verify();
    }

    @Test
    void streamsFileWithoutCreatingAByteArrayResponse() {
        server.expect(requestTo("https://example.supabase.co/storage/v1/object/authenticated/documents/users/42/unique"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] { 1, 2 }, MediaType.APPLICATION_OCTET_STREAM));
        var output = new ByteArrayOutputStream();

        storage.downloadTo("users/42/unique", output);

        assertThat(output.toByteArray()).containsExactly(1, 2);
        server.verify();
    }

    @Test
    void deletesOnlyTheSpecifiedObject() {
        server.expect(requestTo("https://example.supabase.co/storage/v1/object/documents"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().json("{\"prefixes\":[\"users/42/unique\"]}"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        storage.delete("users/42/unique");

        server.verify();
    }

    @Test
    void storageFailureDoesNotExposeTheUpstreamBody() {
        server.expect(anything()).andRespond(withServerError().body("private upstream details"));

        assertThatThrownBy(() -> storage.upload("file",
                new MockMultipartFile("file", new byte[] { 1 }), "text/plain"))
                .isInstanceOf(StorageException.class)
                .hasMessage("Supabase Storage upload failed")
                .hasMessageNotContaining("private upstream details");
    }

    @Test
    void missingConfigurationIsRejectedClearly() {
        SupabaseStorageService missing = new SupabaseStorageService("", "", "");

        assertThatThrownBy(missing::getBucket)
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("configuration is missing");
    }
}
