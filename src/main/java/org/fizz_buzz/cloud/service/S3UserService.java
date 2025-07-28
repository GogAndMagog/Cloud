package org.fizz_buzz.cloud.service;

import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.exception.ResourceNotFound;
import org.fizz_buzz.cloud.repository.S3Repository;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

@Service
public class S3UserService {

    private final static String DEFAULT_BUCKET_NAME = "user-files";
    private final static String USER_DIRECTORY = "user-%d-files/";

    private final S3Repository s3Repository;

    public S3UserService(S3Repository s3Repository) {
        this.s3Repository = s3Repository;
    }

    public void createUserBucketIfNotExist() {

        if (!s3Repository.isBucketExists(DEFAULT_BUCKET_NAME)) {
            s3Repository.createBucket(DEFAULT_BUCKET_NAME);
        }
    }

    public ResourceInfoResponseDTO getResource(long userId, String resourcePath) {

        try {

            var response = s3Repository.getResourceInfo(DEFAULT_BUCKET_NAME,
                    USER_DIRECTORY.formatted(userId).concat(resourcePath));

            return new ResourceInfoResponseDTO(response.path().substring(USER_DIRECTORY.formatted(userId).length()),
                    response.name(),
                    response.size(),
                    response.type());
        } catch (ResourceNotFound e) {
            throw new ResourceNotFound(resourcePath);
        }
    }

    public void deleteResource(long userId, String resourcePath) {

        if (!s3Repository.isObjectExists(DEFAULT_BUCKET_NAME, USER_DIRECTORY.formatted(userId).concat(resourcePath))) {

            throw new ResourceNotFound(resourcePath);
        }

        s3Repository.deleteResource(DEFAULT_BUCKET_NAME, USER_DIRECTORY.formatted(userId).concat(resourcePath));
    }

    public void createUserDirectory(long userId) {

        s3Repository.createDirectory(DEFAULT_BUCKET_NAME, USER_DIRECTORY.formatted(userId));
    }

    public InputStream downloadResource(long userId, String resourcePath) {

        return s3Repository.download(DEFAULT_BUCKET_NAME, USER_DIRECTORY.formatted(userId).concat(resourcePath));
    }
}
