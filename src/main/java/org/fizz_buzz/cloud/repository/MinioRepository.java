package org.fizz_buzz.cloud.repository;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.exception.EmptyPathException;
import org.fizz_buzz.cloud.exception.ForbiddenSymbolException;
import org.fizz_buzz.cloud.exception.ResourceNotFound;
import org.fizz_buzz.cloud.exception.S3RepositoryException;
import org.fizz_buzz.cloud.model.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
@Validated
public class MinioRepository implements S3Repository {

    private final MinioClient minioClient;

    @Override
    public void createBucket(String bucketName) {

        try {

            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteBucket(String bucketName) {

        try {

            var objects = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .recursive(true)
                    .build());

            List<DeleteObject> deleteObjects = new LinkedList<>();

            for (Result<Item> object : objects) {
                deleteObjects.add(new DeleteObject(object.get().objectName()));
            }

            var result =
                    minioClient.removeObjects(RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(deleteObjects)
                            .build());

            //needed for eager removal of objects
            for (Result<DeleteError> deleteErrorResult : result) {
            }

            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isBucketExists(String bucketName) {

        try {

            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> findAllNamesByPrefix(String bucket, String prefix, boolean recursive) {

        List<String> names = new ArrayList<>();

        var directoryObjects = minioClient.listObjects(ListObjectsArgs
                .builder()
                .bucket(bucket)
                .prefix(prefix)
                .recursive(recursive)
                .build());


        for (Result<Item> directoryObject : directoryObjects) {

            try {

                names.add(directoryObject.get().objectName());
            } catch (Exception e) {

                throw new S3RepositoryException(e);
            }
        }

        return names;
    }

    @Override
    public void deleteResource(String bucketName, String path) {

        isValidPath(path);

        try {

            if (path.endsWith("/")) {

                var directoryObjects = minioClient.listObjects(ListObjectsArgs
                        .builder()
                        .bucket(bucketName)
                        .prefix(path)
                        .recursive(true)
                        .build());

                List<DeleteObject> deleteObjects = new LinkedList<>();

                for (Result<Item> object : directoryObjects) {
                    deleteObjects.add(new DeleteObject(object.get().objectName()));
                }

                var result = minioClient.removeObjects(RemoveObjectsArgs
                        .builder()
                        .bucket(bucketName)
                        .objects(deleteObjects)
                        .build());

                //needed for eager removal of objects
                for (Result<DeleteError> deleteErrorResult : result) {
                }
            } else {

                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(path)
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Resource getResourceByPath(String bucket, String path) {

        try {

            var objectStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .build());

            var objects = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(path)
                    .build());

            if (objects.iterator().hasNext()) {

                var objectInfo = objects.iterator().next();

                return new Resource(objectStream.object(), objectStream, objectInfo.get().size());
            } else {
                throw new ResourceNotFound(path);
            }
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                throw new ResourceNotFound(path);
            } else {
                throw new S3RepositoryException(e);
            }
        } catch (Exception e) {
            throw new S3RepositoryException(e);
        }
    }

    public void saveResource(String bucket, String path, InputStream dataStream) {

        try {

            minioClient.putObject(PutObjectArgs
                    .builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(dataStream, -1, 10485760)
                    .build());
        } catch (Exception e) {
            throw new S3RepositoryException(e);
        }
    }

    @Override
    public void createDirectory(String bucketName, String path) {

        List<String> directories = new ArrayList<>();

        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                directories.add(path.substring(0, i + 1));
            }
        }

        directories.add(path);

        try {

            for (String directory : directories) {

                if (!isObjectExists(bucketName, directory)) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(directory)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public boolean isObjectExists(String bucketName, String path) {

        isValidPath(path);

        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
        } catch (ErrorResponseException e) {

            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            } else {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private void isValidPath(String path) {

        String forbiddenSymbols = ".*[\\\\/?*:<>\"|].*";

        if (path == null || path.isBlank()) {
            throw new EmptyPathException();
        }

        List<String> directories = new ArrayList<>();
        String fileName = "";
        int prevSlash = 0;

        for (int i = 0; i < path.length(); i++) {

            if (path.charAt(i) == '/') {
                directories.add(path.substring(prevSlash, i));
                prevSlash = i + 1;
            }
        }

        if (directories.stream().anyMatch(String::isEmpty)) {
            throw new EmptyPathException();
        }

        if (directories.stream().anyMatch(directory -> Pattern.matches(forbiddenSymbols, directory))) {
            throw new ForbiddenSymbolException();
        }

        if (!path.endsWith("/")) {
            fileName = path.substring(prevSlash);

            if (Pattern.matches(forbiddenSymbols, fileName)) {
                throw new ForbiddenSymbolException();
            }
        }
    }
}
