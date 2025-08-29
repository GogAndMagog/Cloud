package org.fizz_buzz.cloud.repository;

public interface S3BucketRepository {

    void create(String name);

    void delete(String name);

    boolean exists(String name);

}