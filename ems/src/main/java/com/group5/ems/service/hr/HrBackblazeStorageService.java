package com.group5.ems.service.hr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class HrBackblazeStorageService {

    @Value("${backblaze.b2.endpoint}")
    private String endpoint;

    @Value("${backblaze.b2.accessKeyId}")
    private String accessKeyId;

    @Value("${backblaze.b2.secretAccessKey}")
    private String secretAccessKey;

    @Value("${backblaze.b2.bucketName}")
    private String bucketName;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (accessKeyId == null || accessKeyId.isBlank() || accessKeyId.contains("REPLACE_WITH")) {
            log.warn("Backblaze B2 Storage is not configured properly. Cloud functions may fail.");
            return;
        }

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(Region.US_EAST_1) // Backblaze doesn't strictly use regions like AWS but SDK requires it
                .serviceConfiguration(b -> b.pathStyleAccessEnabled(true))
                .build();
    }

    /**
     * Uploads report bytes to Backblaze B2.
     * @param fileName The key/name of the file in the bucket.
     * @param content The byte content of the PDF.
     * @return The key of the uploaded file.
     */
    public String uploadReport(String fileName, byte[] content) {
        validateConfig();
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("application/pdf")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
            log.info("""
                    Successfully uploaded report to Backblaze: {}
                    """, fileName);
            return fileName;
        } catch (S3Exception e) {
            log.error("""
                    Failed to upload report to Backblaze: {}
                    """, e.getMessage());
            throw new RuntimeException("Cloud storage upload failed", e);
        }
    }

    /**
     * Downloads report bytes from Backblaze B2.
     * @param fileName The key/name of the file in the bucket.
     * @return Optional containing byte array if found.
     */
    public Optional<byte[]> downloadReport(String fileName) {
        if (s3Client == null) return Optional.empty();
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return Optional.of(objectBytes.asByteArray());
        } catch (NoSuchKeyException e) {
            log.warn("""
                    Report not found in Backblaze: {}
                    """, fileName);
            return Optional.empty();
        } catch (S3Exception e) {
            log.error("""
                    Error downloading from Backblaze: {}
                    """, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Deletes a report from Backblaze B2.
     * @param fileName The key/name of the file in the bucket.
     */
    public void deleteReport(String fileName) {
        if (s3Client == null) return;
        
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("""
                    Successfully deleted report from Backblaze: {}
                    """, fileName);
        } catch (S3Exception e) {
            log.error("""
                    Failed to delete report from Backblaze: {}
                    """, e.getMessage());
            throw new RuntimeException("Cloud storage deletion failed", e);
        }
    }

    private void validateConfig() {
        if (s3Client == null) {
            throw new IllegalStateException("""
                    Backblaze B2 Storage Client is not initialized. Check your application.properties configuration.
                    """);
        }
    }
}
