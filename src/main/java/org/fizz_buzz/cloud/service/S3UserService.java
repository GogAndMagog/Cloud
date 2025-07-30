package org.fizz_buzz.cloud.service;

import org.fizz_buzz.cloud.dto.ResourceType;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.exception.ResourceNotFound;
import org.fizz_buzz.cloud.model.Resource;
import org.fizz_buzz.cloud.repository.S3Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
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

            var resource = s3Repository.getResourceByPath(DEFAULT_BUCKET_NAME,
                    USER_DIRECTORY.formatted(userId).concat(resourcePath));

            return new ResourceInfoResponseDTO(resource.path().substring(USER_DIRECTORY.formatted(userId).length()),
                    resource.path(),
                    resource.size(),
                    isDirectory(resource.path()) ? ResourceType.DIRECTORY : ResourceType.FILE);
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

    public StreamingResponseBody downloadResource(long userId, String resourcePath) {

        // Needed for correct queries to Minio
        String technicalPath = USER_DIRECTORY.formatted(userId).concat(resourcePath);

        List<Resource> resources = new ArrayList<>();
        List<String> resourcesNames = s3Repository.findAllNamesByPrefix(DEFAULT_BUCKET_NAME, technicalPath);

        for (String resourcesName : resourcesNames) {
            resources.add(s3Repository.getResourceByPath(DEFAULT_BUCKET_NAME, resourcesName));
        }

        // redundantOffset needed to cut unnecessary information about user directory and directories
        // that higher than target directory
        Path entirePath = Paths.get(USER_DIRECTORY.formatted(userId).concat(resourcePath));

        int redundantOffset = Math.abs(entirePath.getFileName().toString().length() - technicalPath.length()) - 1;

        return outputStream -> {

            if (resources.size() == 1) {

                Resource resource = resources.getFirst();

                try (InputStream resourceStream = resource.dataStream()) {

                    writeStream(outputStream, resourceStream);
                } catch (IOException e) {

                    throw new RuntimeException(e);
                }
            } else {

                ZipEntry zipEntry;

                try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {

                    for (Resource resource : resources) {

                        try (InputStream resourceStream = resource.dataStream()) {

                            zipEntry = new ZipEntry(resource.path().substring(redundantOffset));
                            zos.putNextEntry(zipEntry);

                            if (!resource.path().endsWith("/")) {
                                writeStream(zos, resourceStream);
                            }

                            zos.closeEntry();
                        }
                    }
                }
            }
        };
    }

    private void writeStream(OutputStream os, InputStream is) throws IOException {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
    }

    private boolean isDirectory(String path){

        return path.endsWith("/");
    }
}
