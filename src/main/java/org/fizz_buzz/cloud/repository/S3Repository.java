package org.fizz_buzz.cloud.repository;

import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.model.Resource;

import java.util.List;

public interface S3Repository {

    void createBucket(String bucketName);
    void deleteBucket(String bucketName);
    boolean isBucketExists(String bucketName);
    void deleteResource(String bucketName, String path);
    List<String> findAllNamesByPrefix(String bucket, String prefix);
    Resource getResourceByPath(String bucket, String path);
    ResourceInfoResponseDTO move(String from, String to);
    List<ResourceInfoResponseDTO> search(String path);
    List<ResourceInfoResponseDTO> upload(String path);
    List<ResourceInfoResponseDTO> directoryInfo(String path);
    List<ResourceInfoResponseDTO> createDirectory(String bucketName, String path);
    boolean isObjectExists(String bucketName, String path);
}