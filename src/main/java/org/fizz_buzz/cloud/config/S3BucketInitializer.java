package org.fizz_buzz.cloud.config;

import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.repository.S3BucketRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class S3BucketInitializer implements ApplicationRunner {

    private final S3BucketRepository s3BucketRepository;

    @Value("${application.user-files-bucket}")
    private String bucket;

    @Override
    public void run(ApplicationArguments args) {
        if (!s3BucketRepository.exists(bucket)) {
            s3BucketRepository.create(bucket);
        }
    }
}
