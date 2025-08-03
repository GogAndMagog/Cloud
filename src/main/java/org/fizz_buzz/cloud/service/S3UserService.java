package org.fizz_buzz.cloud.service;

import org.fizz_buzz.cloud.dto.ResourceType;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.exception.DirectoryNotExistException;
import org.fizz_buzz.cloud.exception.ResourceAlreadyExistsException;
import org.fizz_buzz.cloud.exception.ResourceNotFound;
import org.fizz_buzz.cloud.exception.S3RepositoryException;
import org.fizz_buzz.cloud.model.Resource;
import org.fizz_buzz.cloud.repository.S3Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

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
        List<String> resourcesNames = s3Repository.findAllNamesByPrefix(DEFAULT_BUCKET_NAME, technicalPath, true);

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

            List<String> names = s3Repository.findAllNamesByPrefix(DEFAULT_BUCKET_NAME, oldTechnicalPath, true);

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

        return s3Repository.findAllNamesByPrefix(DEFAULT_BUCKET_NAME, USER_DIRECTORY.formatted(userId), true)
                .stream()
                .filter(name -> name.substring(userDirectory.length()).toLowerCase().contains(query.toLowerCase()))
                .map(name -> s3Repository.getResourceByPath(DEFAULT_BUCKET_NAME, name))
                .map(resource -> resourceToResourceInfoResponseDTO(userId, resource))
                .collect(Collectors.toList());
    }

    public List<ResourceInfoResponseDTO> upload(long userId, String uploadPath, MultipartFile[] files) {

        String technicalPath = USER_DIRECTORY.formatted(userId).concat(uploadPath);
        List<ResourceInfoResponseDTO> response = new ArrayList<>();

        // uploading path validation
        if (!uploadPath.isBlank() &&
                (!isDirectory(uploadPath) || !s3Repository.isObjectExists(DEFAULT_BUCKET_NAME, technicalPath))) {
            throw new DirectoryNotExistException(uploadPath);
        }

        Set<String> directories = new HashSet<>();

        // collecting all directories
        for (MultipartFile file : files) {

            if (file.getOriginalFilename() != null) {

                // resource validation
                if (s3Repository.isObjectExists(DEFAULT_BUCKET_NAME, technicalPath.concat(file.getOriginalFilename()))) {
                    throw new ResourceAlreadyExistsException(file.getOriginalFilename());
                }

                for (int i = 0; i < file.getOriginalFilename().length(); i++) {

                    if (file.getOriginalFilename().charAt(i) == '\\') {

                        directories.add(technicalPath.concat(file.getOriginalFilename().substring(0, i)));
                    }
                }

                directories.add(file.getOriginalFilename());
            }
        }

        // crating nonexistent directories
        for (String directory : directories) {

            if (!s3Repository.isObjectExists(DEFAULT_BUCKET_NAME, directory)) {
                s3Repository.createDirectory(DEFAULT_BUCKET_NAME, directory);
            }
        }

        // file uploading
        for (MultipartFile file : files) {

            if (file.getOriginalFilename() != null) {

                try (InputStream dataStream = file.getInputStream()) {

                    s3Repository.saveResource(DEFAULT_BUCKET_NAME,
                            technicalPath.concat(file.getOriginalFilename()),
                            dataStream);
                    response.add(resourceToResourceInfoResponseDTO(userId, s3Repository.getResourceByPath(DEFAULT_BUCKET_NAME,
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

        if (!s3Repository.isObjectExists(DEFAULT_BUCKET_NAME, technicalName)) {
            throw new ResourceNotFound(path);
        }

        return s3Repository.findAllNamesByPrefix(DEFAULT_BUCKET_NAME, technicalName, false)
                .stream()
                // we need to cut user directory if it is root directory e.i. path is empty
                // or cut searching directory
                .filter(name -> !name.equals(USER_DIRECTORY.formatted(userId)) && (path.isBlank() || !name.endsWith(path)))
                .map(name -> s3Repository.getResourceByPath(DEFAULT_BUCKET_NAME, name))
                .map(resource -> resourceToResourceInfoResponseDTO(userId, resource))
                .collect(Collectors.toList());
    }

    public ResourceInfoResponseDTO createDirectory(long userId, String path) {

        if (!isDirectory(path)) {
            throw new NotDirectoryException(path);
        }

        String technicalName = USER_DIRECTORY.formatted(userId).concat(path);

        if (s3Repository.isObjectExists(DEFAULT_BUCKET_NAME, technicalName)) {
            throw new ResourceAlreadyExistsException(path);
        }

        s3Repository.createDirectory(DEFAULT_BUCKET_NAME, technicalName);

        return resourceToResourceInfoResponseDTO(userId,
                s3Repository.getResourceByPath(DEFAULT_BUCKET_NAME, technicalName));
    }

    private ResourceInfoResponseDTO resourceToResourceInfoResponseDTO(long userId, Resource resource) {

        Path fullPath = Paths.get(resource.path());

        ResourceType resourceType = isDirectory(resource.path()) ? ResourceType.DIRECTORY : ResourceType.FILE;
        String path;
        String fileName = fullPath.getFileName().toString();;

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

    private void writeStream(OutputStream os, InputStream is) throws IOException {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
    }

    private boolean isDirectory(String path) {

        return path.endsWith("/") && path.length() > 1;
    }
}
