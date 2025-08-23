package org.fizz_buzz.cloud.integration;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.minio.MinioClient;
import org.fizz_buzz.cloud.dto.ResourceType;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.repository.MinioRepository;
import org.fizz_buzz.cloud.repository.S3Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@Disabled("First attempt to use Testcontainers. Disabled cause of my laziness.")
public class S3RepositoryTest {

    private static final String S3_USER = "minioadmin";
    private static final String S3_PASS = "minioadmin";

    private static final String BUCKET_TEST = "testbucket";
    private static final String DIRECTORY_TEST = "aaa/";
    private static final String NESTED_DIRECTORY_TEST = "aaa/ggg/";


    @Container
    private static final MinIOContainer minioContainer = new MinIOContainer("minio/minio:latest")
            .withPassword(S3_PASS)
            .withUserName(S3_USER)
            .withExposedPorts(9000)
            .withCreateContainerCmdModifier(createContainerCmd ->
                    createContainerCmd
                            .getHostConfig()
                            .withPortBindings(new PortBinding(Ports.Binding.bindPort(9000)
                                    , new ExposedPort(9000))));
    private static MinioClient minioClient;

    private S3Repository s3Repository;

    @BeforeAll
    public static void setUp() {

        minioClient = MinioClient.builder()
                .endpoint(minioContainer.getS3URL())
                .credentials(S3_USER, S3_PASS)
                .build();
    }

    static class S3RepositoryProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) throws Exception {

            return Stream.of(Arguments.of(new MinioRepository(minioClient)));
        }
    }

    @Nested
    @ParameterizedClass
    @ArgumentsSource(S3RepositoryTest.S3RepositoryProvider.class)
    class BucketOperations {

        @Parameter
        S3Repository s3Repository;

        @Test
        public void s3Service_CreateBucket_Success() {

            s3Repository.createBucket(BUCKET_TEST);

            assertTrue(s3Repository.isBucketExists(BUCKET_TEST));

            s3Repository.deleteBucket(BUCKET_TEST);
        }

        @Test
        public void s3Service_DeleteBucket_Success() {

            s3Repository.createBucket(BUCKET_TEST);

            s3Repository.deleteBucket(BUCKET_TEST);

            assertFalse(s3Repository.isBucketExists(BUCKET_TEST));
        }


    }

    @Nested
    @ParameterizedClass
    @ArgumentsSource(S3RepositoryTest.S3RepositoryProvider.class)
    class ObjectOperations {

        @Parameter
        S3Repository s3Repository;

        @BeforeEach
        public void setUp() {

            s3Repository.createBucket(BUCKET_TEST);
        }

        @Test
        public void s3Service_CreateDirectory_Success() {

            s3Repository.createDirectory(BUCKET_TEST, DIRECTORY_TEST);

            assertTrue(s3Repository.isObjectExists(BUCKET_TEST, DIRECTORY_TEST));
        }

        @Test
        public void s3Service_CreateNestedDirectory_Success() {

            s3Repository.createDirectory(BUCKET_TEST, NESTED_DIRECTORY_TEST);

            assertTrue(s3Repository.isObjectExists(BUCKET_TEST, NESTED_DIRECTORY_TEST));
        }

        @Test
        public void s3Service_GetResourceInfo_Success() {

            ResourceInfoResponseDTO pattern = new ResourceInfoResponseDTO(DIRECTORY_TEST,
                    "",
                    0L,
                    ResourceType.DIRECTORY);

            s3Repository.createDirectory(BUCKET_TEST, DIRECTORY_TEST);

//            var response = s3Repository.getResourceInfo(BUCKET_TEST, DIRECTORY_TEST);

//            assertEquals(pattern, response);
        }

        @Test
        public void s3Service_DeleteNestedDirectories_Success() {

            s3Repository.createDirectory(BUCKET_TEST, "aaa/bbb/ccc/");

            s3Repository.deleteResource(BUCKET_TEST, "aaa/");

            assertAll(() -> assertFalse(s3Repository.isObjectExists(BUCKET_TEST, "aaa/")),
                    () -> assertFalse(s3Repository.isObjectExists(BUCKET_TEST, "aaa/bbb/")),
                    () -> assertFalse(s3Repository.isObjectExists(BUCKET_TEST, "aaa/bbb/ccc/")));
        }

        @Test
        public void s3Service_DeleteNestedDirectories_NonNullException() {

            assertThrows(Exception.class, () -> s3Repository.deleteResource(BUCKET_TEST, null));
        }


        @Test
        public void s3Service_DeleteNestedDirectories_NotEmptyException() {

            assertThrows(Exception.class, () -> s3Repository.deleteResource(BUCKET_TEST, ""));
        }

        @Test
        public void s3Service_DeleteNestedDirectories_PathIsNotCorrect() {

//            Validator

//            assertThrows(Exception.class, () -> s3Repository.deleteResource(BUCKET_TEST, "aaa/ccc..."));
        }


        @AfterEach
        void tearDown() {

            s3Repository.deleteBucket(BUCKET_TEST);
        }
    }
}
