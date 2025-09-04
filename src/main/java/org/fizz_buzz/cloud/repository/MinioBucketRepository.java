package org.fizz_buzz.cloud.repository;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MinioBucketRepository implements S3BucketRepository {

    private final MinioClient minioClient;

    @Override
    public void create(String name) {
        try {
            MakeBucketArgs request = MakeBucketArgs.builder()
                    .bucket(name)
                    .build();

            minioClient.makeBucket(request);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String name) {

        try {
            RemoveBucketArgs request = RemoveBucketArgs.builder()
                    .bucket(name)
                    .build();

            minioClient.removeBucket(request);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists(String name) {
        BucketExistsArgs request = BucketExistsArgs.builder()
                .bucket(name)
                .build();

        try {
            return minioClient.bucketExists(request);
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw new RuntimeException(e);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new RuntimeException(e);
        }
    }
}
