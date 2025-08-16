package org.fizz_buzz.cloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.MessageDTO;
import org.fizz_buzz.cloud.dto.request.UserRequestDTO;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.dto.response.UserResponseDTO;
import org.fizz_buzz.cloud.security.CustomUserDetails;
import org.fizz_buzz.cloud.service.S3UserService;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Tag(
        name = "Resource management",
        description = "This part is a standard REST API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resource")
public class ResourceController {

    private final S3UserService s3UserService;


    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    }

    @Operation(
            summary = "Get resource info",
            description = "Exhibit info about concrete resource if it exists.",
            parameters = @Parameter(
                    name = "path",
                    description = "Path must point to concrete resource"
            ),
            responses = {
                    @ApiResponse(
                            description = "Registration success",
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ResourceInfoResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Validation error or path doesn't exist",
                            responseCode = "400",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Resource not found",
                            responseCode = "404",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    )
            }
    )
    @GetMapping
    public ResourceInfoResponseDTO getResource(@Valid
                                               @RequestParam(name = "path")
                                               @NotBlank(message = "Parameter \"path\" must not be blank") String path,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {

        return s3UserService.getResource(userDetails.getId(), path);
    }


    @Operation(
            summary = "Delete resource",
            description = "Delete resource by path prefix.",
            parameters = @Parameter(
                    name = "path",
                    description = "Prefix can be concrete file or directory"
            ),
            responses = {
                    @ApiResponse(
                            description = "Successful deletion",
                            responseCode = "204"
                    ),
                    @ApiResponse(
                            description = "Validation error or path doesn't exist",
                            responseCode = "400",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Resource not found",
                            responseCode = "404",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    )
            }
    )
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@Valid
                               @RequestParam(name = "path")
                               @NotBlank(message = "Parameter \"path\" must not be blank") String path,
                               @AuthenticationPrincipal CustomUserDetails userDetails) {

        s3UserService.deleteResource(userDetails.getId(), path);
    }


    @Operation(
            summary = "Download resource",
            description = "Download resource to client. If resource is directory then download file is zip-archive.",
            parameters = @Parameter(
                    name = "path",
                    description = "Path must point to concrete resource"
            ),
            responses = {
                    @ApiResponse(
                            description = "Download success",
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE
                            )
                    ),
                    @ApiResponse(
                            description = "Validation error or path doesn't exist",
                            responseCode = "400",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Resource not found",
                            responseCode = "404",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    )
            }
    )
    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadResource(@Valid
                                                                  @RequestParam(name = "path")
                                                                  @NotBlank(message = "Parameter \"path\" must not be blank") String path,
                                                                  @AuthenticationPrincipal CustomUserDetails userDetails) throws UnsupportedEncodingException {

        StreamingResponseBody streamingResponseBody = s3UserService.downloadResource(userDetails.getId(), path);

        Path entirePath = Paths.get(path);
        String fileName;

        if (path.endsWith("/")) {
            fileName = entirePath.getFileName().toString().concat(".zip");
        } else {
            fileName = entirePath.getFileName().toString();
        }

        // needed to support other languages, not only English
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''%s".formatted(encodedFileName))
                .body(streamingResponseBody);
    }

    @Operation(
            summary = "Move resource",
            description = """
                    In case of moving resource change only resource path.
                    In case of renaming resource change only resource name.""",
            parameters = {
                    @Parameter(
                            name = "from",
                            description = "Old resource path"
                    ),
                    @Parameter(
                            name = "to",
                            description = "New resource path"
                    ),
            },
            responses = {
                    @ApiResponse(
                            description = "Successful resource move",
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ResourceInfoResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Validation error or path doesn't exist",
                            responseCode = "400",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Resource not found",
                            responseCode = "404",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Resource at new path already exist",
                            responseCode = "409",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    )
            }
    )
    @GetMapping("/move")
    public ResourceInfoResponseDTO move(@Valid
                                        @RequestParam(name = "from")
                                        @NotBlank(message = "Parameter \"from\" must not be blank") String from,
                                        @Valid
                                        @RequestParam(name = "to")
                                        @NotBlank(message = "Parameter \"to\" must not be blank") String to,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {

        return s3UserService.moveResource(userDetails.getId(), from, to);
    }


    @Operation(
            summary = "Search resource",
            description = """
                    Case insensitive search by part or complete resource name.""",
            parameters = @Parameter(
                    name = "query",
                    description = "Part or complete resource name"
            ),
            responses = {
                    @ApiResponse(
                            description = "Resource found",
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = ResourceInfoResponseDTO.class)
                                    )
                            )
                    ),
                    @ApiResponse(
                            description = "Validation error or path doesn't exist",
                            responseCode = "400",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    )
            }
    )
    @GetMapping("/search")
    public List<ResourceInfoResponseDTO> search(@Valid
                                                @RequestParam(name = "query")
                                                @NotBlank(message = "Parameter \"query\" must not be blank")
                                                String query,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {

        return s3UserService.searchResource(userDetails.getId(), query);
    }


    @Operation(
            summary = "Upload resource",
            description = """
                    Either file or directory can be uploaded to the system.""",
            parameters = {
                    @Parameter(
                            name = "path",
                            description = "Path where resource must be uploaded"
                    ),
                    @Parameter(
                            name = "object",
                            description = "Multipart file or files",
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation = MultipartFile.class
                                    )
                            )
                    )
            },
            responses = {
                    @ApiResponse(
                            description = "Resource uploaded",
                            responseCode = "201",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = ResourceInfoResponseDTO.class)
                                    )
                            )
                    ),
                    @ApiResponse(
                            description = "Validation error or path doesn't exist",
                            responseCode = "400",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),

                    @ApiResponse(
                            description = "Resource already existed",
                            responseCode = "409",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    )
            }
    )
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceInfoResponseDTO> upload(@RequestParam(name = "path") String path,
                                                @RequestParam(name = "object") MultipartFile[] files,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {

        return s3UserService.upload(userDetails.getId(), path, files);
    }
}
