package org.fizz_buzz.cloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.response.ErrorMessageResponseDto;
import org.fizz_buzz.cloud.dto.request.DeleteResourcePathRequestParam;
import org.fizz_buzz.cloud.dto.request.GetResourceRequestParam;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.security.CustomUserDetails;
import org.fizz_buzz.cloud.service.StorageService;
import org.fizz_buzz.cloud.util.PathUtils;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.ContentDisposition;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@Tag(name = "Resource management", description = "This part is a standard REST API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resource")
public class ResourceController {

    private final StorageService storageService;


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
                                    schema = @Schema(implementation = ResourceInfoResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Validation error or path doesn't exist",
                            responseCode = "400",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Resource not found",
                            responseCode = "404",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    )
            }
    )
    @GetMapping
    public ResourceInfoResponseDTO getResource(@Valid @RequestParam("path") GetResourceRequestParam request,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        return storageService.getResource(userDetails.getId(), request.getPath());
    }


    @Operation(summary = "Delete resource", description = "Delete resource by path prefix.")
    @ApiResponses({
            @ApiResponse(
                    description = "Successful deletion", responseCode = "204"),
            @ApiResponse(
                    description = "Validation error or path doesn't exist", responseCode = "400",
                    content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))),
            @ApiResponse(
                    description = "Unauthorized user", responseCode = "401",
                    content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))),
            @ApiResponse(
                    description = "Resource not found", responseCode = "404",
                    content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))),
            @ApiResponse(
                    description = "Internal server error", responseCode = "500",
                    content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class)))
    })
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@Valid @RequestParam("path") DeleteResourcePathRequestParam request,
                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        storageService.deleteResource(userDetails.getId(), request.getPath());
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
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Resource not found",
                            responseCode = "404",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    )
            }
    )
    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadResource(@Valid @RequestParam("path") String path,
                                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {

        StreamingResponseBody streamingResponseBody = storageService.downloadResource(userDetails.getId(), path);

        String filename;
        if (PathUtils.isDirectory(path)) {
            filename = PathUtils.extractFilename(path) + ".zip";
        } else {
            filename = PathUtils.extractFilename(path);
        }

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename)
                .build();

        return ResponseEntity.ok()
                .headers(headers -> headers.setContentDisposition(contentDisposition))
                .body(streamingResponseBody);
    }

    @Operation(
            summary = "Move resource",
            description = " In case of moving resource change only resource path. In case of renaming resource change only resource name.",
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
                                    schema = @Schema(implementation = ResourceInfoResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Validation error or path doesn't exist",
                            responseCode = "400",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Resource not found",
                            responseCode = "404",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Resource at new path already exist",
                            responseCode = "409",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    )
            }
    )
    @GetMapping("/move")
    public ResourceInfoResponseDTO move(@Valid
                                        @RequestParam("from")
                                        @NotBlank(message = "Parameter \"from\" must not be blank") String from,
                                        @Valid
                                        @RequestParam("to")
                                        @NotBlank(message = "Parameter \"to\" must not be blank") String to,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {

        return storageService.moveResource(userDetails.getId(), from, to);
    }


    @Operation(
            summary = "Search resource",
            description = "Case insensitive search by part or complete resource name.",
            parameters = @Parameter(
                    name = "query",
                    description = "Part or complete resource name"
            ),
            responses = {
                    @ApiResponse(
                            description = "Resource found",
                            responseCode = "200",
                            content = @Content(
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = ResourceInfoResponseDTO.class)
                                    )
                            )
                    ),
                    @ApiResponse(
                            description = "Validation error or path doesn't exist",
                            responseCode = "400",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    )
            }
    )
    @GetMapping("/search")
    public List<ResourceInfoResponseDTO> search(@Valid @RequestParam("query") @NotBlank(message = "Parameter \"query\" must not be blank")
                                                String query,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {

        return storageService.searchResource(userDetails.getId(), query);
    }


    @Operation(
            summary = "Upload resource",
            description = "Either file or directory can be uploaded to the system.",
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
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = ResourceInfoResponseDTO.class)
                                    )
                            )
                    ),
                    @ApiResponse(
                            description = "Validation error or path doesn't exist",
                            responseCode = "400",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),

                    @ApiResponse(
                            description = "Resource already existed",
                            responseCode = "409",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    )
            }
    )
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceInfoResponseDTO> upload(@RequestParam("path") String path,
                                                @RequestPart("object") List<MultipartFile> files,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return storageService.upload(userDetails.getId(), path, files);
    }
}
