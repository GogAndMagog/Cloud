package org.fizz_buzz.cloud.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.exception.DirectoryNotExistException;
import org.fizz_buzz.cloud.exception.EmptyFilenameException;
import org.fizz_buzz.cloud.exception.NotDirectoryException;
import org.fizz_buzz.cloud.exception.ResourceAlreadyExistsException;
import org.fizz_buzz.cloud.exception.ResourceNotFoundException;
import org.fizz_buzz.cloud.exception.S3RepositoryException;
import org.fizz_buzz.cloud.mapper.ResourceInfoMapper;
import org.fizz_buzz.cloud.model.ResourceInfo;
import org.fizz_buzz.cloud.repository.S3BucketRepository;
import org.fizz_buzz.cloud.repository.S3ObjectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final static String USER_DIRECTORY_TEMPLATE = "user-%d-files/";

    @Value("${application.default-bucket-name}")
    private String bucketName;

    private final S3ObjectRepository s3ObjectRepository;
    private final S3BucketRepository s3BucketRepository;
    private final ResourceInfoMapper resourceInfoMapper;


    public void createUserBucketIfNotExist() {
        if (!s3BucketRepository.exists(bucketName)) {
            s3BucketRepository.create(bucketName);
        }
    }

    public ResourceInfoResponseDTO getResource(long userId, String resourcePath) {
        String fullPath = USER_DIRECTORY_TEMPLATE.formatted(userId).concat(resourcePath);

        return s3ObjectRepository.findInfoByPath(bucketName, fullPath)
                .map(resourceInfoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(resourcePath));
    }

    public void deleteResource(long userId, String resourcePath) {
        String fullPath = USER_DIRECTORY_TEMPLATE.formatted(userId).concat(resourcePath);

        if (!s3ObjectRepository.existsByPath(bucketName, fullPath)) {
            throw new ResourceNotFoundException(resourcePath);
        }

        if (isDirectory(resourcePath)) {
            List<String> directoryEntries = s3ObjectRepository.findAllInfoByPrefix(bucketName, fullPath, true)
                    .stream()
                    .map(ResourceInfo::getKey)
                    .toList();

            s3ObjectRepository.deleteAll(bucketName, directoryEntries);
        } else {
            s3ObjectRepository.delete(bucketName, fullPath);
        }
    }

    public void createUserDirectory(long userId) {
        String userDirectory = USER_DIRECTORY_TEMPLATE.formatted(userId);

        ResourceInfo resourceInfo = new ResourceInfo(userDirectory, -1L);
        InputStream directoryFlag = new ByteArrayInputStream(new byte[0]);

        s3ObjectRepository.save(bucketName, resourceInfo, directoryFlag);
    }

    public StreamingResponseBody downloadResource(long userId, String resourcePath) {
        String fullPath = USER_DIRECTORY_TEMPLATE.formatted(userId).concat(resourcePath);

        if (!isDirectory(resourcePath)) {
            return outputStream -> {
                try (InputStream resource = s3ObjectRepository.getByPath(bucketName, fullPath)) {
                    resource.transferTo(outputStream);
                }
            };
        }

        int filenameOffset = fullPath.length() - Paths.get(fullPath).getFileName().toString().length();

        return outputStream -> {
            ZipOutputStream zos = new ZipOutputStream(outputStream);

            List<String> resources = s3ObjectRepository.findAllInfoByPrefix(bucketName, fullPath, true)
                    .stream()
                    .map(ResourceInfo::getKey)
                    .toList();

            for (String fullPathToFile : resources) {
                InputStream resource = s3ObjectRepository.getByPath(bucketName, fullPathToFile);

                String zipEntryName = fullPathToFile.substring(filenameOffset);
                ZipEntry zipEntry = new ZipEntry(zipEntryName);
                zos.putNextEntry(zipEntry);

                if (!isDirectory(fullPathToFile)) {
                    resource.transferTo(zos);
                }

                zos.closeEntry();
                resource.close();
            }
            zos.finish();
            zos.close();
        };
    }

    public ResourceInfoResponseDTO moveResource(long userId, String oldPath, String newPath) {
        String userDirectory = USER_DIRECTORY_TEMPLATE.formatted(userId);

        String oldFullPath = userDirectory.concat(oldPath);

        if (!s3ObjectRepository.existsByPath(bucketName, oldFullPath)) {
            throw new ResourceNotFoundException(oldPath);
        }

        String newFullPath = userDirectory.concat(newPath);

        if (s3ObjectRepository.existsByPath(bucketName, newFullPath)) {
            throw new ResourceAlreadyExistsException(newPath);
        }

        if (isDirectory(oldPath)) {
            s3ObjectRepository.findAllInfoByPrefix(bucketName, oldFullPath, true)
                    .stream()
                    .map(ResourceInfo::getKey)
                    .forEach(key -> s3ObjectRepository.copy(bucketName, key, key.replace(oldFullPath, newFullPath)));
        } else {
            s3ObjectRepository.copy(bucketName, oldFullPath, newFullPath);
        }

        s3ObjectRepository.delete(bucketName, oldFullPath);

        return s3ObjectRepository.findInfoByPath(bucketName, newFullPath)
                .map(resourceInfoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(newFullPath));
    }

    public List<ResourceInfoResponseDTO> searchResource(long userId, String query) {
        String userDirectory = USER_DIRECTORY_TEMPLATE.formatted(userId);

        return s3ObjectRepository.findAllInfoByPrefix(bucketName, userDirectory, true)
                .stream()
                .filter(info -> StringUtils.containsIgnoreCase(
                        info.getKey().replace(userDirectory, ""), query
                ))
                .map(resourceInfoMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<ResourceInfoResponseDTO> upload(long userId, String uploadPath, List<MultipartFile> files) {

        if (!uploadPath.isBlank() && !isDirectory(uploadPath)) {
            throw new NotDirectoryException(uploadPath);
        }

        String rootPath = USER_DIRECTORY_TEMPLATE.formatted(userId).concat(uploadPath);

        if (!s3ObjectRepository.existsByPath(bucketName, rootPath)) {
            throw new DirectoryNotExistException(uploadPath);
        }

        Set<String> directories = new HashSet<>();
        Map<ResourceInfo, InputStream> resources = new HashMap<>();

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();

            if (StringUtils.isBlank(filename)) {
                throw new EmptyFilenameException("Filename cannot be empty");
            }

            if (s3ObjectRepository.existsByPath(bucketName, rootPath.concat(filename))) {
                throw new ResourceAlreadyExistsException(filename);
            }

            for (int i = 0; i < filename.length(); i++) {

                if (filename.charAt(i) == '/') {

                    String directory = rootPath.concat(filename.substring(0, i + 1));

                    if (s3ObjectRepository.existsByPath(bucketName, directory)) {
                        throw new ResourceAlreadyExistsException(filename);
                    }
                    directories.add(directory);
                }
            }


            if (isDirectory(filename)) {
                directories.add(filename);
            } else {
                String fullPathToFile = rootPath.concat(filename);
                ResourceInfo resourceInfo = new ResourceInfo(fullPathToFile, file.getSize());
                try {
                    resources.put(resourceInfo, file.getInputStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        for (String directory : directories) {
            ResourceInfo directoryInfo = new ResourceInfo(directory, -1L);
            InputStream directoryFlag = new ByteArrayInputStream(new byte[0]);
            s3ObjectRepository.save(bucketName, directoryInfo, directoryFlag);
        }

        if (resources.size() == 1) {
            resources.forEach((resourceInfo, dataStream) -> {
                try (dataStream) {
                    s3ObjectRepository.save(bucketName, resourceInfo, dataStream);
                } catch (IOException e) {
                    throw new S3RepositoryException(e);
                }
            });
        } else {
            s3ObjectRepository.saveAll(bucketName, resources);
        }

        return resources.keySet().stream()
                .map(ResourceInfo::getKey)
                .map(key -> s3ObjectRepository.findInfoByPath(bucketName, key))
                .flatMap(Optional::stream)
                .map(resourceInfoMapper::toDto)
                .toList();
    }

    public List<ResourceInfoResponseDTO> getDirectory(long userId, String path) {

        if (!isDirectory(path) && !path.isBlank()) {
            throw new NotDirectoryException(path);
        }

        String currentUserDirectory = USER_DIRECTORY_TEMPLATE.formatted(userId);
        String fullPath = currentUserDirectory.concat(path);

        if (!s3ObjectRepository.existsByPath(bucketName, fullPath)) {
            throw new ResourceNotFoundException(path);
        }

        return s3ObjectRepository.findAllInfoByPrefix(bucketName, fullPath, false)
                .stream()
                .filter(info -> !info.getKey().equals(fullPath))
                .map(resourceInfoMapper::toDto)
                .collect(Collectors.toList());
    }

    public ResourceInfoResponseDTO createDirectory(long userId, String path) {

        if (!isDirectory(path)) {
            throw new NotDirectoryException(path);
        }

        String fullPath = USER_DIRECTORY_TEMPLATE.formatted(userId).concat(path);

        if (s3ObjectRepository.existsByPath(bucketName, path)) {
            throw new ResourceAlreadyExistsException(fullPath);
        }

        ResourceInfo directoryInfo = new ResourceInfo(fullPath, -1L);
        ByteArrayInputStream directoryFlag = new ByteArrayInputStream(new byte[0]);

        s3ObjectRepository.save(bucketName, directoryInfo, directoryFlag);

        return s3ObjectRepository.findInfoByPath(bucketName, fullPath)
                .map(resourceInfoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(fullPath));
    }

    private boolean isDirectory(String path) {
        return path.endsWith("/");
    }
}
