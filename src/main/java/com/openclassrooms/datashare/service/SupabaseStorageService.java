package com.openclassrooms.datashare.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class SupabaseStorageService {
    private final RestClient client;
    private final String bucket;

    @Autowired
    public SupabaseStorageService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.secret-key:}") String key,
            @Value("${supabase.storage.bucket:}") String bucket) {
        this.bucket = bucket;
        if (supabaseUrl.isBlank() || key.isBlank() || bucket.isBlank()) {
            this.client = null;
            return;
        }
        this.client = RestClient.builder()
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader("apikey", key)
                .build();
    }

    SupabaseStorageService(RestClient client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    public String getBucket() {
        requireConfigured();
        return bucket;
    }

    public void upload(String path, MultipartFile file, String contentType) {
        requireConfigured();
        try {
            client.post().uri("/object/{bucket}/{path}", bucket, path)
                    .header("x-upsert", "false")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(file.getSize())
                    .body(file.getResource()).retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw new StorageException("Supabase Storage upload failed");
        }
    }

    public byte[] download(String path) {
        requireConfigured();
        try {
            byte[] content = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/object/authenticated/{bucket}")
                            .pathSegment(path.split("/"))
                            .build(bucket))
                    .retrieve()
                    .body(byte[].class);

            if (content == null) {
                throw new StorageException("Supabase Storage returned no file content");
            }
            return content;
        } catch (RestClientException exception) {
            throw new StorageException("Supabase Storage download failed");
        }
    }

    public void delete(String path) {
        requireConfigured();
        try {
            client.method(HttpMethod.DELETE).uri("/object/{bucket}", bucket)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(path))).retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw new StorageException("Supabase Storage cleanup failed");
        }
    }

    private void requireConfigured() {
        if (client == null) {
            throw new StorageException("Supabase Storage configuration is missing");
        }
    }
}
