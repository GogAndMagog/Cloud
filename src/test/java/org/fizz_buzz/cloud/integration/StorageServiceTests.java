package org.fizz_buzz.cloud.integration;


import org.fizz_buzz.cloud.exception.NotDirectoryException;
import org.fizz_buzz.cloud.repository.S3Repository;
import org.fizz_buzz.cloud.service.StorageService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;


public class StorageServiceTests extends IntegrationTestBaseClass {

    private static final String DEFAULT_BUCKET = "user-files";
    private final static String USER_DIRECTORY = "user-%d-files/";

    @Autowired
    private StorageService storageService;
    @Autowired
    private S3Repository s3Repository;
    private static long currentUserId = 0;

    @Test
    void createUserDirectory_UserId_Success() {

        long userId = nextUserId();

        storageService.createUserDirectory(userId);

        assertTrue(s3Repository.isObjectExists(DEFAULT_BUCKET, getTechnicalName(userId, "")));
    }

    @Test
    void createDefaultBucket_SpringContextStarts_Success() {

        assertTrue(s3Repository.isBucketExists(DEFAULT_BUCKET));
    }

    @Test
    void createDirectory_CorrectName_Success() {

        long userId = nextUserId();
        String directoryName = "Test/";

        storageService.createUserDirectory(userId);
        storageService.createDirectory(userId, directoryName);

        assertTrue(s3Repository.isObjectExists(DEFAULT_BUCKET, getTechnicalName(userId, directoryName)));
    }

    @Test
    void createDirectory_DirectoryNameWithoutSlash_NotDirectoryException() {

        long userId = nextUserId();
        String directoryName = "Test";

        storageService.createUserDirectory(userId);

        assertThrows(NotDirectoryException.class, () -> storageService.createDirectory(userId, directoryName));
    }

    @Test
    @Disabled("Validation logic need to be added")
    void createDirectory_DirectoryNameContainsForbiddenSymbols_NotDirectoryException() {

        long userId = nextUserId();
        String directoryName = "Test::<>/";

        storageService.createUserDirectory(userId);

        assertThrows(NotDirectoryException.class, () -> storageService.createDirectory(userId, directoryName));
    }

    @Test
    void createDirectory_NestedDirectory_Success() {

        long userId = nextUserId();
        String directoryName = "Test/Nested/";

        storageService.createUserDirectory(userId);
        storageService.createDirectory(userId, directoryName);

        assertAll(
                () -> assertTrue(s3Repository.isObjectExists(DEFAULT_BUCKET, getTechnicalName(userId, "Test/"))),
                () -> assertTrue(s3Repository.isObjectExists(DEFAULT_BUCKET, getTechnicalName(userId, directoryName)))
        );
    }

    private long nextUserId() {

        return currentUserId++;
    }

    private String getTechnicalName(long userId, String resourcePath) {

        return USER_DIRECTORY.formatted(userId).concat(resourcePath);
    }
}
