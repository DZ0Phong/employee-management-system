package com.group5.ems.service.guest;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

@Service
public class FileStorageService {

    private final MinioClient minioClient;

    private final String bucket = "cv-storage";

    public FileStorageService() {

        this.minioClient = MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("admin", "12345678")
                .build();
    }

    public String uploadFile(MultipartFile file) {

        try {

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            return fileName;

        } catch (Exception e) {
            throw new RuntimeException("Upload CV failed", e);
        }
    }
}