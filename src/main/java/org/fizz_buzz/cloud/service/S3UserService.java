package org.fizz_buzz.cloud.service;

import org.fizz_buzz.cloud.repository.S3Repository;
import org.springframework.stereotype.Service;

@Service
public class S3UserService {

    private final static String DEFAULT_BUCKET_NAME = "user-files";
    private final static String USER_DIRECTORY = "user-%d-files/";

    private final S3Repository s3Repository;

    public S3UserService(S3Repository s3Repository) {
        this.s3Repository = s3Repository;
    }

    public void createUserBucketIfNotExist(){

        if (!s3Repository.isBucketExists(DEFAULT_BUCKET_NAME)){
            s3Repository.createBucket(DEFAULT_BUCKET_NAME);
        }
    }

    public void createUserDirectory(long userId){

        s3Repository.createDirectory(DEFAULT_BUCKET_NAME, USER_DIRECTORY.formatted(userId));
    }
}
