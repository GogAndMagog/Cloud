package org.fizz_buzz.cloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.fizz_buzz.cloud.validation.annotation.Path;

@Data
public class GetResourceRequestParam {

    @Path
    @NotBlank(message = "Parameter 'path' must not be blank")
    private String path;
}
