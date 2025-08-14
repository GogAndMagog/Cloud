package org.fizz_buzz.cloud.service;

import org.apache.commons.io.IOUtils;
import org.fizz_buzz.cloud.dto.ResourceType;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.exception.DirectoryNotExistException;
import org.fizz_buzz.cloud.exception.ResourceAlreadyExistsException;
import org.fizz_buzz.cloud.exception.ResourceNotFound;
import org.fizz_buzz.cloud.exception.S3RepositoryException;
import org.fizz_buzz.cloud.model.Resource;
import org.fizz_buzz.cloud.repository.S3Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.fizz_buzz.cloud.exception.NotDirectoryException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@PropertySource("classpath:application.properties")
public class S3UserService {

    private final static String USER_DIRECTORY = "user-%d-files/";

    @Value("${application.default-bucket-name}")
    private String defaultBucketName;

    private final S3Repository s3Repository;

    public S3UserService(S3Repository s3Repository) {
        this.s3Repository = s3Repository;
    }

    public void createUserBucketIfNotExist() {

        if (!s3Repository.isBucketExists(defaultBucketName)) {
            s3Repository.createBucket(defaultBucketName);
        }
    }

    public ResourceInfoResponseDTO getResource(long userId, String resourcePath) {

        try {

            var resource = s3Repository.getResourceByPath(defaultBucketName,
                    USER_DIRECTORY.formatted(userId).concat(resourcePath));

            return resourceToResourceInfoResponseDTO(userId, resource);
        } catch (ResourceNotFound e) {
            throw new ResourceNotFound(resourcePath);
        }
    }

    public void deleteResource(long userId, String resourcePath) {

        if (!s3Repository.isObjectExists(defaultBucketName, USER_DIRECTORY.formatted(userId).concat(resourcePath))) {

            throw new ResourceNotFound(resourcePath);
        }

        s3Repository.deleteResource(defaultBucketName, USER_DIRECTORY.formatted(userId).concat(resourcePath));
    }

    public void createUserDirectory(long userId) {

        s3Repository.createDirectory(defaultBucketName, USER_DIRECTORY.formatted(userId));
    }

    public StreamingResponseBody downloadResource(long userId, String resourcePath) {

        // Needed for correct queries to Minio
        String technicalPath = USER_DIRECTORY.formatted(userId).concat(resourcePath);

        List<Resource> resources = new ArrayList<>();
        List<String> resourcesNames = s3Repository.findAllNamesByPrefix(defaultBucketName, technicalPath, true);

        for (String resourcesName : resourcesNames) {
            resources.add(s3Repository.getResourceByPath(defaultBucketName, resourcesName));
        }

        // redundantOffset needed to cut unnecessary information about user directory and directories
        // that higher than target directory
        Path entirePath = Paths.get(USER_DIRECTORY.formatted(userId).concat(resourcePath));

        int redundantOffset = technicalPath.length() - entirePath.getFileName().toString().length() - 1;

        if (resources.size() == 1) {
            return outputStream -> {
                Resource resource = resources.getFirst();

                try (InputStream resourceStream = resource.dataStream()) {

                    IOUtils.copy(resourceStream, outputStream, 1024);
                } catch (IOException e) {

                    throw new RuntimeException(e);
                }
            };
        } else {
            return outputStream -> {
                ZipEntry zipEntry;

                try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {

                    for (Resource resource : resources) {

                        try (InputStream resourceStream = resource.dataStream()) {

                            zipEntry = new ZipEntry(resource.path().substring(redundantOffset));
                            zos.putNextEntry(zipEntry);

                            if (!isDirectory(resource.path())) {
                                IOUtils.copy(resourceStream, zos, 1024);
                            }

                            zos.closeEntry();
                        }
                    }
                }
            };
        }
    }

    public ResourceInfoResponseDTO moveResource(long userId, String oldPath, String newPath) {

        String oldTechnicalPath = USER_DIRECTORY.formatted(userId).concat(oldPath);
        String newTechnicalPath = USER_DIRECTORY.formatted(userId).concat(newPath);

        if (!s3Repository.isObjectExists(defaultBucketName, oldTechnicalPath)) {

            throw new ResourceNotFound(oldPath);
        }

        if (s3Repository.isObjectExists(defaultBucketName, newTechnicalPath)) {

            throw new ResourceAlreadyExistsException(newPath);
        }

        Resource resource;

        if (isDirectory(oldPath)) {

            List<String> names = s3Repository.findAllNamesByPrefix(defaultBucketName, oldTechnicalPath, true);

            for (String name : names) {

                resource = s3Repository.getResourceByPath(defaultBucketName, name);
                s3Repository.saveResource(defaultBucketName,
                        name.replace(oldTechnicalPath, newTechnicalPath),
                        resource.dataStream());
            }
        } else {

            resource = s3Repository.getResourceByPath(defaultBucketName, oldTechnicalPath);
            s3Repository.saveResource(defaultBucketName, newTechnicalPath, resource.dataStream());
        }

        s3Repository.deleteResource(defaultBucketName, oldTechnicalPath);

        resource = s3Repository.getResourceByPath(defaultBucketName, newTechnicalPath);

        return resourceToResourceInfoResponseDTO(userId, resource);
    }

    public List<ResourceInfoResponseDTO> searchResource(long userId, String query) {

        String userDirectory = USER_DIRECTORY.formatted(userId);

        return s3Repository.findAllNamesByPrefix(defaultBucketName, USER_DIRECTORY.formatted(userId), true)
                .stream()
                .filter(name -> name.substring(userDirectory.length()).toLowerCase().contains(query.toLowerCase()))
                .map(name -> s3Repository.getResourceByPath(defaultBucketName, name))
                .map(resource -> resourceToResourceInfoResponseDTO(userId, resource))
                .collect(Collectors.toList());
    }

    public List<ResourceInfoResponseDTO> upload(long userId, String uploadPath, MultipartFile[] files) {

        String technicalPath = USER_DIRECTORY.formatted(userId).concat(uploadPath);
        List<ResourceInfoResponseDTO> response = new ArrayList<>();

        // uploading path validation
        if (!uploadPath.isBlank() &&
                (!isDirectory(uploadPath) || !s3Repository.isObjectExists(defaultBucketName, technicalPath))) {
            throw new DirectoryNotExistException(uploadPath);
        }

        Set<String> directories = new HashSet<>();

        // collecting all directories
        for (MultipartFile file : files) {

            if (file.getOriginalFilename() != null) {

                // resource validation
                if (s3Repository.isObjectExists(defaultBucketName, technicalPath.concat(file.getOriginalFilename()))) {
                    throw new ResourceAlreadyExistsException(file.getOriginalFilename());
                }

                for (int i = 0; i < file.getOriginalFilename().length(); i++) {

                    if (file.getOriginalFilename().charAt(i) == '/') {

                        String directory = technicalPath.concat(file.getOriginalFilename().substring(0, i + 1));

                        // do not allow upload directories that already exists
                        if (s3Repository.isObjectExists(defaultBucketName, directory)) {

                            throw new ResourceAlreadyExistsException(file.getOriginalFilename().substring(0, i + 1));
                        } else {
                            directories.add(directory);
                        }
                    }
                }

                if (isDirectory(file.getOriginalFilename())) {
                    directories.add(file.getOriginalFilename());
                }
            }
        }

        // crating nonexistent directories
        for (String directory : directories) {

            if (!s3Repository.isObjectExists(defaultBucketName, directory)) {
                s3Repository.createDirectory(defaultBucketName, directory);
            }
        }

        // file uploading
        for (MultipartFile file : files) {

            if (file.getOriginalFilename() != null) {

                try (InputStream dataStream = file.getInputStream()) {

                    s3Repository.saveResource(defaultBucketName,
                            technicalPath.concat(file.getOriginalFilename()),
                            dataStream);
                    response.add(resourceToResourceInfoResponseDTO(userId, s3Repository.getResourceByPath(defaultBucketName,
                            technicalPath.concat(file.getOriginalFilename()))));
                } catch (Exception e) {

                    throw new S3RepositoryException(e);
                }
            }
        }

        return response;
    }

    public List<ResourceInfoResponseDTO> getDirectory(long userId, String path) {

        if (!isDirectory(path) && !path.isBlank()) {
            throw new NotDirectoryException(path);
        }

        String technicalName = USER_DIRECTORY.formatted(userId).concat(path);

        if (!s3Repository.isObjectExists(defaultBucketName, technicalName)) {
            throw new ResourceNotFound(path);
        }

        return s3Repository.findAllNamesByPrefix(defaultBucketName, technicalName, false)
                .stream()
                // we need to cut user directory if it is root directory e.i. path is empty
                // or cut searching directory
                .filter(name -> !name.equals(USER_DIRECTORY.formatted(userId)) && (path.isBlank() || !name.equals(technicalName)))
                .map(name -> s3Repository.getResourceByPath(defaultBucketName, name))
                .map(resource -> resourceToResourceInfoResponseDTO(userId, resource))
                .collect(Collectors.toList());
    }

    public ResourceInfoResponseDTO createDirectory(long userId, String path) {

        if (!isDirectory(path)) {
            throw new NotDirectoryException(path);
        }

        String technicalName = USER_DIRECTORY.formatted(userId).concat(path);

        if (s3Repository.isObjectExists(defaultBucketName, technicalName)) {
            throw new ResourceAlreadyExistsException(path);
        }

        s3Repository.createDirectory(defaultBucketName, technicalName);

        return resourceToResourceInfoResponseDTO(userId,
                s3Repository.getResourceByPath(defaultBucketName, technicalName));
    }

    private ResourceInfoResponseDTO resourceToResourceInfoResponseDTO(long userId, Resource resource) {

        Path fullPath = Paths.get(resource.path());

        ResourceType resourceType = isDirectory(resource.path()) ? ResourceType.DIRECTORY : ResourceType.FILE;
        String path;
        String fileName = fullPath.getFileName().toString();

        if (resourceType == ResourceType.DIRECTORY) {

            // It is necessary to take into account '/' at the end of directory path
            path = resource.path().substring(USER_DIRECTORY.formatted(userId).length(),
                    resource.path().length() - fileName.length() - 1);
        } else {

            path = resource.path().substring(USER_DIRECTORY.formatted(userId).length(),
                    resource.path().length() - fileName.length());
        }

        return new ResourceInfoResponseDTO(path, fileName, resource.size(), resourceType);
    }

    private boolean isDirectory(String path) {

        return path.endsWith("/") && path.length() > 1;
    }
}
