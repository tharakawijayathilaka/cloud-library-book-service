package com.cloudlibrary.bookservice.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class CloudStorageService {

    @Value("${gcs.project-id:#{null}}")
    private String projectId;

    @Value("${gcs.bucket-name:#{null}}")
    private String bucketName;

    @Value("${gcs.credentials-path:#{null}}")
    private String credentialsPath;

    @Value("${storage.provider:local}")
    private String storageProvider;

    private Storage storage;

    @PostConstruct
    public void init() {
        if ("gcs".equalsIgnoreCase(storageProvider) && projectId != null) {
            try {
                StorageOptions.Builder builder = StorageOptions.newBuilder()
                        .setProjectId(projectId);

                if (credentialsPath != null && !credentialsPath.isEmpty()) {
                    builder.setCredentials(
                            GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                    );
                }
                // If no credentials path, uses Application Default Credentials (ADC)

                storage = builder.build().getService();
                System.out.println("GCS Storage initialized. Bucket: " + bucketName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize GCS", e);
            }
        } else {
            System.out.println("Storage provider set to LOCAL. GCS disabled.");
        }
    }

    /**
     * Upload a file to GCS and return the public URL.
     * Falls back to a placeholder URL if GCS is not configured.
     */
    public String uploadFile(MultipartFile file) throws IOException {
        if (storage == null || bucketName == null) {
            // Local fallback - return a placeholder
            return "https://storage.googleapis.com/BUCKET_PLACEHOLDER/" + file.getOriginalFilename();
        }

        String fileName = "covers/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
    }

    /**
     * Delete a file from GCS by its full URL.
     */
    public void deleteFile(String fileUrl) {
        if (storage == null || bucketName == null || fileUrl == null) {
            return;
        }

        try {
            // Extract object name from URL
            String prefix = "https://storage.googleapis.com/" + bucketName + "/";
            if (fileUrl.startsWith(prefix)) {
                String objectName = fileUrl.substring(prefix.length());
                storage.delete(BlobId.of(bucketName, objectName));
            }
        } catch (Exception e) {
            System.err.println("Failed to delete file from GCS: " + e.getMessage());
        }
    }
}
