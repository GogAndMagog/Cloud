package org.fizz_buzz.cloud.config;

import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.service.StorageService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppInitializer implements ApplicationRunner {

    private final StorageService storageService;

    @Override
    public void run(ApplicationArguments args) {
        storageService.createUserBucketIfNotExist();
    }
}
