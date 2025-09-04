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
import org.fizz_buzz.cloud.repository.S3ObjectRepository;
import org.fizz_buzz.cloud.util.PathUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    private final S3ObjectRepository s3ObjectRepository;
    private final ResourceInfoMapper resourceInfoMapper;


    public ResourceInfoResponseDTO getResource(long userId, String resourcePath) {
        String fullPath = USER_DIRECTORY_TEMPLATE.formatted(userId).concat(resourcePath);

        return s3ObjectRepository.findInfoByPath(fullPath)
                .map(resourceInfoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(resourcePath));
    }

    public void deleteResource(long userId, String resourcePath) {
        String fullPath = USER_DIRECTORY_TEMPLATE.formatted(userId).concat(resourcePath);

        if (!existsByPath(fullPath)) {
            throw new ResourceNotFoundException(resourcePath);
        }

        if (PathUtils.isDirectory(resourcePath)) {
            List<String> directoryEntries = s3ObjectRepository.findAllInfoByPrefix(fullPath, true)
                    .stream()
                    .map(ResourceInfo::getKey)
                    .toList();

            s3ObjectRepository.deleteAll(directoryEntries);
        } else {
            s3ObjectRepository.delete(fullPath);
        }
    }

    public StreamingResponseBody downloadResource(long userId, String resourcePath) {
        String fullPath = USER_DIRECTORY_TEMPLATE.formatted(userId).concat(resourcePath);

        if (!PathUtils.isDirectory(resourcePath)) {
            return outputStream -> {
                try (InputStream resource = s3ObjectRepository.getByPath(fullPath)) {
                    resource.transferTo(outputStream);
                }
            };
        }

        int filenameOffset = fullPath.length() - PathUtils.extractFilename(fullPath).length();

        return outputStream -> {
            ZipOutputStream zos = new ZipOutputStream(outputStream);

            List<ResourceInfo> resources = s3ObjectRepository.findAllInfoByPrefix(fullPath, true);

            for (ResourceInfo resourceInfo : resources) {
                String fullPathToFile = resourceInfo.getKey();

                try (InputStream resource = s3ObjectRepository.getByPath(fullPathToFile)) {
                    String zipEntryName = fullPathToFile.substring(filenameOffset);
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zos.putNextEntry(zipEntry);

                    if (!PathUtils.isDirectory(fullPathToFile)) {
                        resource.transferTo(zos);
                    }

                    zos.closeEntry();
                }
            }
            zos.finish();
            zos.close();
        };
    }

    public ResourceInfoResponseDTO moveResource(long userId, String oldPath, String newPath) {
        String userDirectory = USER_DIRECTORY_TEMPLATE.formatted(userId);

        String oldFullPath = userDirectory.concat(oldPath);

        if (!existsByPath(oldFullPath)) {
            throw new ResourceNotFoundException(oldPath);
        }

        String newFullPath = userDirectory.concat(newPath);

        if (existsByPath(newFullPath)) {
            throw new ResourceAlreadyExistsException(newPath);
        }

        if (PathUtils.isDirectory(oldPath)) {
            s3ObjectRepository.findAllInfoByPrefix(oldFullPath, true)
                    .stream()
                    .map(ResourceInfo::getKey)
                    .forEach(key -> s3ObjectRepository.copy(key, key.replace(oldFullPath, newFullPath)));
        } else {
            s3ObjectRepository.copy(oldFullPath, newFullPath);
        }

        s3ObjectRepository.delete(oldFullPath);

        return s3ObjectRepository.findInfoByPath(newFullPath)
                .map(resourceInfoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(newFullPath));
    }

    public List<ResourceInfoResponseDTO> searchResource(long userId, String query) {
        String userDirectory = USER_DIRECTORY_TEMPLATE.formatted(userId);

        return s3ObjectRepository.findAllInfoByPrefix(userDirectory, true)
                .stream()
                .filter(info -> StringUtils.containsIgnoreCase(
                        info.getKey().replace(userDirectory, ""), query
                ))
                .map(resourceInfoMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<ResourceInfoResponseDTO> upload(long userId, String uploadPath, List<MultipartFile> files) {
        if (!PathUtils.isDirectory(uploadPath)) {
            throw new NotDirectoryException(uploadPath);
        }

        String rootPath = USER_DIRECTORY_TEMPLATE.formatted(userId).concat(uploadPath);

        if (!existsByPath(rootPath)) {
            throw new DirectoryNotExistException(uploadPath);
        }

        Map<ResourceInfo, InputStream> resources = new HashMap<>();
        Set<String> directories = new HashSet<>();

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();

            if (StringUtils.isBlank(filename)) {
                throw new EmptyFilenameException("Filename cannot be empty");
            }

            if (existsByPath(rootPath.concat(filename))) {
                throw new ResourceAlreadyExistsException(filename);
            }

            directories.addAll(PathUtils.extractInnerDirectories(filename));

            if (!PathUtils.isDirectory(rootPath)) {
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
            String fullPathToDirectory = rootPath.concat(directory);

            if (existsByPath(fullPathToDirectory)) {
                throw new ResourceAlreadyExistsException(directory);
            }

            ResourceInfo directoryInfo = new ResourceInfo(fullPathToDirectory, -1L);
            InputStream directoryObject = new ByteArrayInputStream(new byte[0]);

            resources.put(directoryInfo, directoryObject);
        }

        if (resources.size() == 1) {
            resources.forEach((resourceInfo, dataStream) -> {
                try (dataStream) {
                    s3ObjectRepository.save(resourceInfo, dataStream);
                } catch (IOException e) {
                    throw new S3RepositoryException(e);
                }
            });
        } else {
            s3ObjectRepository.saveAll(resources);
        }

        return resources.keySet().stream()
                .map(ResourceInfo::getKey)
                .filter(path -> !PathUtils.isDirectory(path))
                .map(s3ObjectRepository::findInfoByPath)
                .flatMap(Optional::stream)
                .map(resourceInfoMapper::toDto)
                .toList();
    }

    private boolean existsByPath(String path) {
        return s3ObjectRepository.findInfoByPath(path).isPresent();
    }

    public void createUserDirectory(long userId) {
        String userDirectory = USER_DIRECTORY_TEMPLATE.formatted(userId);

        ResourceInfo directoryInfo = new ResourceInfo(userDirectory, -1L);
        InputStream directoryObject = new ByteArrayInputStream(new byte[0]);

        s3ObjectRepository.save(directoryInfo, directoryObject);
    }

    public List<ResourceInfoResponseDTO> getDirectory(long userId, String path) {
        if (!PathUtils.isDirectory(path) && !path.isBlank()) {
            throw new NotDirectoryException(path);
        }

        String currentUserDirectory = USER_DIRECTORY_TEMPLATE.formatted(userId);
        String fullPath = currentUserDirectory.concat(path);

        if (!existsByPath(fullPath)) {
            throw new ResourceNotFoundException(path);
        }

        return s3ObjectRepository.findAllInfoByPrefix(fullPath, false)
                .stream()
                .filter(info -> !info.getKey().equals(fullPath))
                .map(resourceInfoMapper::toDto)
                .collect(Collectors.toList());
    }

    public ResourceInfoResponseDTO createDirectory(long userId, String path) {

        if (!PathUtils.isDirectory(path)) {
            throw new NotDirectoryException(path);
        }

        String fullPath = USER_DIRECTORY_TEMPLATE.formatted(userId).concat(path);

        if (existsByPath(path)) {
            throw new ResourceAlreadyExistsException(fullPath);
        }

        ResourceInfo directoryInfo = new ResourceInfo(fullPath, -1L);
        InputStream directoryObject = new ByteArrayInputStream(new byte[0]);

        s3ObjectRepository.save(directoryInfo, directoryObject);

        return s3ObjectRepository.findInfoByPath(fullPath)
                .map(resourceInfoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(fullPath));
    }
}
