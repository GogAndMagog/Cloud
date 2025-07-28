package org.fizz_buzz.cloud.repository;

import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

public interface S3Repository {

    void createBucket(String bucketName);
    void deleteBucket(String bucketName);
    boolean isBucketExists(String bucketName);
    ResourceInfoResponseDTO getResourceInfo(String bucketName, String path);
    void deleteResource(String bucketName, String path);
    InputStream download(String bucket, String path);
    ResourceInfoResponseDTO move(String from, String to);
    List<ResourceInfoResponseDTO> search(String path);
    List<ResourceInfoResponseDTO> upload(String path);
    List<ResourceInfoResponseDTO> directoryInfo(String path);
    List<ResourceInfoResponseDTO> createDirectory(String bucketName, String path);
    boolean isObjectExists(String bucketName, String path);
}