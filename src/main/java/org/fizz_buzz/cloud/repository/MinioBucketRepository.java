package org.fizz_buzz.cloud.repository;

import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MinioBucketRepository implements S3BucketRepository {

    private final MinioClient minioClient;

    @Override
    public void create(String name) {
        try {
            MakeBucketArgs request = MakeBucketArgs.builder()
                    .bucket(name)
                    .build();

            minioClient.makeBucket(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String name) {

        try {
            ListObjectsArgs listObjectsRequest = ListObjectsArgs.builder()
                    .bucket(name)
                    .recursive(true)
                    .build();

            var objects = minioClient.listObjects(listObjectsRequest);

            List<DeleteObject> deleteObjects = new LinkedList<>();

            for (Result<Item> object : objects) {
                deleteObjects.add(new DeleteObject(object.get().objectName()));
            }

            RemoveObjectsArgs removeObjectsRequest = RemoveObjectsArgs.builder()
                    .bucket(name)
                    .objects(deleteObjects)
                    .build();

            var result = minioClient.removeObjects(removeObjectsRequest);
            for (Result<DeleteError> deleteErrorResult : result) {
                deleteErrorResult.get();
            }

            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(name)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists(String name) {
        BucketExistsArgs request = BucketExistsArgs.builder()
                .bucket(name)
                .build();

        try {
            return minioClient.bucketExists(request);
        } catch (Exception e) {
            return false;
        }
    }
}
