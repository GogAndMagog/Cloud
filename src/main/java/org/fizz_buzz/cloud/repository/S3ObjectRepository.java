package org.fizz_buzz.cloud.repository;

import org.fizz_buzz.cloud.model.ResourceInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface S3ObjectRepository {

    void delete(String bucket, String path);

    void deleteAll(String bucket, List<String> paths);

    List<ResourceInfo> findAllInfoByPrefix(String bucket, String prefix, boolean recursive);

    InputStream getByPath(String bucket, String path);

    void save(String bucket, ResourceInfo resourceInfo, InputStream dataStream);

    Optional<ResourceInfo> findInfoByPath(String bucket, String path);

    boolean existsByPath(String bucket, String path);

    void copy(String bucket, String existingPath, String newPath);

    void saveAll(String bucket, Map<ResourceInfo, InputStream> files);
}