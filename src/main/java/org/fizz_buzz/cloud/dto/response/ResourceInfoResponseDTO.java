package org.fizz_buzz.cloud.dto.response;

import lombok.Builder;
import org.fizz_buzz.cloud.dto.ResourceType;

@Builder
public record ResourceInfoResponseDTO(
        String path,
        String name,
        Long size,
        ResourceType type
) {
}
