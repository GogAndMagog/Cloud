package org.fizz_buzz.cloud.request;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fizz_buzz.cloud.validation.annotation.Directory;
import org.fizz_buzz.cloud.validation.annotation.Path;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetDirectoryContentRequestParam {

    @Path
    @Directory
    @Parameter(name = "path", description = "Path where resource must be uploaded")
    private String path;
}
