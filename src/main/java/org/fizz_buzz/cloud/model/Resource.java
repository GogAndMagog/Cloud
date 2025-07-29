package org.fizz_buzz.cloud.model;

import java.io.InputStream;

public record Resource(String path, InputStream dataStream) {
}
