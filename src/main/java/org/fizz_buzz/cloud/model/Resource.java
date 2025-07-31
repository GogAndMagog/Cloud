package org.fizz_buzz.cloud.model;

import lombok.SneakyThrows;

import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.util.Objects;

public record Resource(String path, InputStream dataStream, long size) {

    private static final Cleaner cleaner = Cleaner.create();

    @SneakyThrows
    public Resource {

        Objects.requireNonNull(dataStream);

        cleaner.register(this, this.dataStream()::close);
    }
}
