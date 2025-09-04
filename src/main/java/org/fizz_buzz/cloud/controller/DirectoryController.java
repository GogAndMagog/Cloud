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
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.response.ErrorMessageResponseDto;
import org.fizz_buzz.cloud.dto.request.CreateDirectoryPathRequestParam;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.dto.request.GetDirectoryContentRequestParam;
import org.fizz_buzz.cloud.security.CustomUserDetails;
import org.fizz_buzz.cloud.service.StorageService;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/directory")
@RequiredArgsConstructor
@Tag(name = "Directory management", description = "This part is a standard REST API")
public class DirectoryController {

    private final StorageService storageService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    }

    @Operation(summary = "Get directory info", description = "Returns collection of resources containing in directory.")
    @ApiResponses({
            @ApiResponse(
                    description = "Resource found",
                    responseCode = "200",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = ResourceInfoResponseDTO.class))
                    )
            ),
            @ApiResponse(
                    description = "Validation error or path doesn't exist", responseCode = "400",
                    content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
            ),
            @ApiResponse(
                    description = "Unauthorized user", responseCode = "401",
                    content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
            ),

            @ApiResponse(
                    description = "Directory does not exists", responseCode = "404",
                    content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
            ),
            @ApiResponse(
                    description = "Internal server error", responseCode = "500",
                    content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
            )
    })
    @GetMapping
    public List<ResourceInfoResponseDTO> getDirectory(@RequestParam("path") @Valid GetDirectoryContentRequestParam request,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {

        return storageService.getDirectory(userDetails.getId(), request.getPath());
    }


    @Operation(
            summary = "Create directory",
            description = """
                    Creates empty directory in system.""",
            parameters =
            @Parameter(
                    name = "path",
                    description = "Path of a new directory"
            ),
            responses = {
                    @ApiResponse(
                            description = "Directory created",
                            responseCode = "201",
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
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Unauthorized user",
                            responseCode = "401",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Parent directory doesn't exists",
                            responseCode = "404",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Directory already existed",
                            responseCode = "409",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorMessageResponseDto.class)
                            )
                    )
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceInfoResponseDTO createDirectory(@RequestParam("path") @Valid CreateDirectoryPathRequestParam request,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return storageService.createDirectory(userDetails.getId(), request.getPath());
    }
}
