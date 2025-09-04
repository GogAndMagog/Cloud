package org.fizz_buzz.cloud.repository;

import org.fizz_buzz.cloud.model.ResourceInfo;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface S3ObjectRepository {

    List<ResourceInfo> findAllInfoByPrefix(String prefix, boolean recursive);

    InputStream getByPath(String path);

    Optional<ResourceInfo> findInfoByPath(String path);

    void copy(String existingPath, String newPath);

    void save(ResourceInfo resourceInfo, InputStream dataStream);

    void saveAll(Map<ResourceInfo, InputStream> objects);

    void delete(String path);

    void deleteAll(List<String> paths);
}