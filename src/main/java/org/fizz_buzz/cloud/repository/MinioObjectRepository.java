package org.fizz_buzz.cloud.repository;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.exception.S3RepositoryException;
import org.fizz_buzz.cloud.model.ResourceInfo;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MinioObjectRepository implements S3ObjectRepository {

    public static final int PART_SIZE = 10485760;

    private final MinioClient minioClient;

    @Override
    public List<ResourceInfo> findAllInfoByPrefix(String bucket, String prefix, boolean recursive) {
        ListObjectsArgs request = ListObjectsArgs.builder()
                .bucket(bucket)
                .prefix(prefix)
                .recursive(recursive)
                .build();

        var directoryObjects = minioClient.listObjects(request);

        List<ResourceInfo> result = new ArrayList<>();
        try {
            for (Result<Item> directoryObject : directoryObjects) {
                Item item = directoryObject.get();
                ResourceInfo resourceInfo = new ResourceInfo(item.objectName(), item.size());
                result.add(resourceInfo);
            }
            return result;
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new S3RepositoryException(e);
        }
    }

    @Override
    public void delete(String bucket, String path) {
        RemoveObjectArgs request = RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(path)
                .build();

        try {
            minioClient.removeObject(request);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new S3RepositoryException(e);
        }
    }

    @Override
    public void deleteAll(String bucket, List<String> paths) {
        List<DeleteObject> objects = paths.stream()
                .map(DeleteObject::new)
                .collect(Collectors.toList());

        RemoveObjectsArgs request = RemoveObjectsArgs
                .builder()
                .bucket(bucket)
                .objects(objects)
                .build();

        try {
            var results = minioClient.removeObjects(request);

            for (Result<DeleteError> deleteError : results) {
                 deleteError.get();
            }
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new S3RepositoryException(e);
        }
    }

    @Override
    public InputStream getByPath(String bucket, String path) {
        GetObjectArgs request = GetObjectArgs.builder()
                .bucket(bucket)
                .object(path)
                .build();

        try {
            return minioClient.getObject(request);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new S3RepositoryException(e);
        }
    }

    @Override
    public void save(String bucket, ResourceInfo resourceInfo, InputStream dataStream) {
        try {
            PutObjectArgs request = PutObjectArgs
                    .builder()
                    .bucket(bucket)
                    .object(resourceInfo.getKey())
                    .stream(dataStream, resourceInfo.getSize(), PART_SIZE)
                    .build();
            minioClient.putObject(request);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new S3RepositoryException(e);
        }
    }

    @Override
    public Optional<ResourceInfo> findInfoByPath(String bucket, String path) {
        StatObjectArgs request = StatObjectArgs.builder()
                .bucket(bucket)
                .object(path)
                .build();

        try {
            StatObjectResponse response = minioClient.statObject(request);
            return Optional.of(new ResourceInfo(response.object(), response.size()));
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return Optional.empty();
            }
            throw new RuntimeException(e);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new S3RepositoryException(e);
        }
    }

    @Override
    public boolean existsByPath(String bucket, String path) {
        return findInfoByPath(bucket, path).isPresent();
    }

    @Override
    public void copy(String bucket, String existingPath, String newPath) {
        CopySource copySource = CopySource.builder()
                .bucket(bucket)
                .object(existingPath)
                .build();

        CopyObjectArgs request = CopyObjectArgs.builder()
                .bucket(bucket)
                .source(copySource)
                .object(newPath)
                .build();
        try {
            minioClient.copyObject(request);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new S3RepositoryException(e);
        }
    }

    @Override
    public void saveAll(String bucket, Map<ResourceInfo, InputStream> resources) {
        try {
            List<SnowballObject> objects = new ArrayList<>();
            resources.forEach((ResourceInfo info, InputStream dataStream) ->
                    objects.add(new SnowballObject(
                            info.getKey(),
                            dataStream,
                            info.getSize(),
                            null
                    ))
            );

            UploadSnowballObjectsArgs request = UploadSnowballObjectsArgs.builder()
                    .bucket(bucket)
                    .objects(objects)
                    .build();

            minioClient.uploadSnowballObjects(request);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new S3RepositoryException(e);
        }
    }
}
