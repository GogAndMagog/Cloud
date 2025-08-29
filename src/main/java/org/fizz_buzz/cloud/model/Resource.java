package org.fizz_buzz.cloud.model;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.util.Objects;

public record Resource(String path, long size) { }
