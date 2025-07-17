package org.fizz_buzz.cloud.config;

import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.service.S3UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppInitializer implements ApplicationRunner {

    private final S3UserService s3UserService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        s3UserService.createUserBucketIfNotExist();
    }
}
