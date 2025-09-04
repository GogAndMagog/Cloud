package org.fizz_buzz.cloud.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PathUtils {

    public boolean isDirectory(String path) {
        return StringUtils.endsWith(path, "/") || StringUtils.isEmpty(path);
    }

    public static String extractFilename(String path) {
        return Path.of(path).getFileName().toString();
    }

    public List<String> extractInnerDirectories(String filename) {
        List<String> directories = new ArrayList<>();

        for (int i = 0; i < filename.length(); i++) {
            if (filename.charAt(i) == '/') {
                directories.add(filename.substring(0, i + 1));
            }
        }

        return directories;
    }
}
