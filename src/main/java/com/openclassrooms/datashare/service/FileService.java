package com.openclassrooms.datashare.service;

import com.openclassrooms.datashare.dto.FileUploadResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    public FileUploadResponseDTO upload(MultipartFile file, String login) {

        return new FileUploadResponseDTO(
                file.getOriginalFilename(), file.getSize(), file.getContentType(), "received");
    }
}
