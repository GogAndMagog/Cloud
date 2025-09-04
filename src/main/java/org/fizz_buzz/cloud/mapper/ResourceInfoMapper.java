package org.fizz_buzz.cloud.mapper;

import org.apache.commons.lang3.StringUtils;
import org.fizz_buzz.cloud.dto.ResourceType;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.model.ResourceInfo;
import org.fizz_buzz.cloud.util.PathUtils;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class ResourceInfoMapper {

    public ResourceInfoResponseDTO toDto(ResourceInfo info) {
        String path = StringUtils.substringAfter(info.getKey(), "/");

        String filename = PathUtils.extractFilename(path);

        if (PathUtils.isDirectory(path)) {
            String pathWithoutLastSlash = StringUtils.removeEnd(path, "/");
            return new ResourceInfoResponseDTO(pathWithoutLastSlash, filename, info.getSize(), ResourceType.DIRECTORY);
        }
        return new ResourceInfoResponseDTO(path, filename, info.getSize(), ResourceType.FILE);
    }

}
