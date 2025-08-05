package org.fizz_buzz.cloud.exception;

public class S3RepositoryException extends RuntimeException{

    public S3RepositoryException() {
    }

    public S3RepositoryException(Throwable cause) {
        super(cause);
    }
}
