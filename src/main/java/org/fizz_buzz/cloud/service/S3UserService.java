package org.fizz_buzz.cloud.service;

import org.apache.tomcat.util.file.Matcher;
import org.fizz_buzz.cloud.dto.ResourceType;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.exception.ResourceAlreadyExistsException;
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
import java.util.stream.Collectors;
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

            return resourceToResourceInfoResponseDTO(userId, resource);
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

    public ResourceInfoResponseDTO moveResource(long userId, String oldPath, String newPath) {

        String oldTechnicalPath = USER_DIRECTORY.formatted(userId).concat(oldPath);
        String newTechnicalPath = USER_DIRECTORY.formatted(userId).concat(newPath);

        if (!s3Repository.isObjectExists(DEFAULT_BUCKET_NAME, oldTechnicalPath)) {

            throw new ResourceNotFound(oldPath);
        }

        if (s3Repository.isObjectExists(DEFAULT_BUCKET_NAME, newTechnicalPath)) {

            throw new ResourceAlreadyExistsException(newPath);
        }

        Resource resource;

        if (isDirectory(oldPath)) {

            List<String> names = s3Repository.findAllNamesByPrefix(DEFAULT_BUCKET_NAME, oldTechnicalPath);

            for (String name : names) {

                resource = s3Repository.getResourceByPath(DEFAULT_BUCKET_NAME, name);
                s3Repository.saveResource(DEFAULT_BUCKET_NAME,
                        name.replace(oldTechnicalPath, newTechnicalPath),
                        resource.dataStream());
            }
        } else {

            resource = s3Repository.getResourceByPath(DEFAULT_BUCKET_NAME, oldTechnicalPath);
            s3Repository.saveResource(DEFAULT_BUCKET_NAME, newTechnicalPath, resource.dataStream());
        }

        s3Repository.deleteResource(DEFAULT_BUCKET_NAME, oldTechnicalPath);

        resource = s3Repository.getResourceByPath(DEFAULT_BUCKET_NAME, newTechnicalPath);

        return resourceToResourceInfoResponseDTO(userId, resource);
    }

    public List<ResourceInfoResponseDTO> searchResource(long userId, String query) {

        String userDirectory = USER_DIRECTORY.formatted(userId);

        return s3Repository.findAllNamesByPrefix(DEFAULT_BUCKET_NAME, USER_DIRECTORY.formatted(userId))
                .stream()
                .filter(name -> name.substring(userDirectory.length()).toLowerCase().contains(query.toLowerCase()))
                .map(name -> s3Repository.getResourceByPath(DEFAULT_BUCKET_NAME, name))
                .map(resource -> resourceToResourceInfoResponseDTO(userId, resource))
                .collect(Collectors.toList());
    }

    private ResourceInfoResponseDTO resourceToResourceInfoResponseDTO(long userId, Resource resource) {

        Path fullPath = Paths.get(resource.path());

        ResourceType resourceType = isDirectory(resource.path()) ? ResourceType.DIRECTORY : ResourceType.FILE;
        String path;
        String fileName;

        if (resourceType == ResourceType.DIRECTORY) {

            fileName = "";
            path = resource.path().substring(USER_DIRECTORY.formatted(userId).length());
        } else {

            fileName = fullPath.getFileName().toString();
            path = resource.path().substring(USER_DIRECTORY.formatted(userId).length(),
                    resource.path().length() - fileName.length());
        }


        return new ResourceInfoResponseDTO(path, fileName, resource.size(), resourceType);
    }

    private void writeStream(OutputStream os, InputStream is) throws IOException {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
    }

    private boolean isDirectory(String path) {

        return path.endsWith("/");
    }
}
