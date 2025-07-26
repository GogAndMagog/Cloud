package org.fizz_buzz.cloud.repository;

import io.minio.BucketExistsArgs;
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
import org.fizz_buzz.cloud.dto.ResourceType;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.exception.EmptyPathException;
import org.fizz_buzz.cloud.exception.ForbiddenSymbolException;
import org.fizz_buzz.cloud.exception.ResourceNotFound;
import org.fizz_buzz.cloud.exception.S3RepositoryException;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

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
    public ResourceInfoResponseDTO getResourceInfo(String bucketName, String path) {

        isValidPath(path);

        var items = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(path)
                .build());

        if (items.iterator().hasNext()) {

            try {
                String folderPath = path.substring(0, path.lastIndexOf("/") + 1);
                String fileName = path.substring(path.lastIndexOf("/") + 1);
                long size = items.iterator().next().get().size();
                ResourceType type = path.endsWith("/") ? ResourceType.DIRECTORY : ResourceType.FILE;

                return new ResourceInfoResponseDTO(folderPath, fileName, size, type);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new ResourceNotFound(path);
        }
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
    public ZipInputStream download(String path, InputStream inputStream) {


        return null;
    }

    @Override
    public ResourceInfoResponseDTO move(String from, String to) {
        return null;
    }

    @Override
    public List<ResourceInfoResponseDTO> search(String path) {
        return List.of();
    }

    @Override
    public List<ResourceInfoResponseDTO> upload(String path) {
        return List.of();
    }

    @Override
    public List<ResourceInfoResponseDTO> directoryInfo(String path) {
        return List.of();
    }

    @Override
    public List<ResourceInfoResponseDTO> createDirectory(String bucketName, String path) {

        List<String> directories = new ArrayList<>();

        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                directories.add(path.substring(0, i + 1));
            }
        }

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

        return List.of();
    }

    public boolean isObjectExists(String bucketName, String path) {

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

        if(directories.stream().anyMatch(String::isEmpty)) {
            throw new EmptyPathException();
        }

        if (directories.stream().anyMatch(directory -> Pattern.matches(forbiddenSymbols, directory))){
            throw new ForbiddenSymbolException();
        }

        if (!path.endsWith("/")){
            fileName = path.substring(prevSlash);

            if (Pattern.matches(forbiddenSymbols, fileName)){
                throw new ForbiddenSymbolException();
            }
        }
    }

    private boolean isDirectoryExists(String bucket, String path) {

        try {

            return minioClient.statObject(StatObjectArgs
                    .builder()
                    .bucket(bucket)
                    .object(path)
                    .build()) != null;
        }
        catch (ErrorResponseException e){
            if (e.errorResponse().code().equals("NoSuckKey")){
                return false;
            }
            else {
                throw new S3RepositoryException(e);
            }
        }
        catch (Exception e) {
            throw new S3RepositoryException(e);
        }
    }
}
