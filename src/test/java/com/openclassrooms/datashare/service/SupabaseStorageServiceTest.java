package com.openclassrooms.datashare.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SupabaseStorageServiceTest {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://example.supabase.co/storage/v1");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SupabaseStorageService storage = new SupabaseStorageService(builder.build(), "documents");

    // @Test
    // void sendsFileBytesToObjectEndpoint() {
    // server.expect(requestTo("https://example.supabase.co/storage/v1/object/documents/users/42/unique"))
    // .andExpect(method(HttpMethod.POST)).andExpect(header("x-upsert", "false"))
    // .andExpect(content().contentType(MediaType.TEXT_PLAIN))
    // .andExpect(content().bytes(new byte[]{1, 2}))
    // .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    // storage.upload("users/42/unique", new MockMultipartFile("file", "a.txt",
    // "text/plain", new byte[]{1, 2}), "text/plain");
    // server.verify();
    // }

    // @Test
    // void deletesOnlySpecifiedObject() {
    // server.expect(requestTo("https://example.supabase.co/storage/v1/object/documents"))
    // .andExpect(method(HttpMethod.DELETE))
    // .andExpect(content().json("{\"prefixes\":[\"users/42/unique\"]}"))
    // .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    // storage.delete("users/42/unique");
    // server.verify();
    // }

    // @Test
    // void storageFailureDoesNotExposeUpstreamBody() {
    // server.expect(anything()).andRespond(withServerError().body("private upstream
    // details"));
    // assertThatThrownBy(() -> storage.upload("file", new MockMultipartFile("file",
    // new byte[]{1}), "text/plain"))
    // .isInstanceOf(StorageException.class).hasMessage("Supabase Storage upload
    // failed");
    // server.verify();
    // }

    // @Test
    // void missingConfigurationRejectsUploadClearly() {
    // var missing = new SupabaseStorageService("", "", "");
    // assertThatThrownBy(missing::getBucket).isInstanceOf(StorageException.class)
    // .hasMessageContaining("configuration is missing");
    // }
}
