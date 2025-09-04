package org.fizz_buzz.cloud.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.fizz_buzz.cloud.validation.annotation.Path;

@Data
public class DeleteResourcePathRequestParam {

    @Path
    @Parameter(name = "path", description = "Prefix can be concrete file or directory")
    private String path;
}
