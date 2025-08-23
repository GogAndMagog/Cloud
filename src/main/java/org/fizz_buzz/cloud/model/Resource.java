package org.fizz_buzz.cloud.model;

import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.util.Objects;

public record Resource(String path, InputStream dataStream, long size) {

    private static final Cleaner cleaner = Cleaner.create();

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run() throws Exception;
    }

    private static void wrap(ThrowingRunnable throwingConsumer) {

        try {

            throwingConsumer.run();
        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    public Resource {

        Objects.requireNonNull(dataStream);

        cleaner.register(this, () -> wrap(dataStream::close));
    }
}
