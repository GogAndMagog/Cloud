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
import kotlin.contracts.Returns;
import lombok.RequiredArgsConstructor;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.fizz_buzz.cloud.dto.MessageDTO;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.request.GetDirectoryContentRequestParam;
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

    @Operation(
            summary = "Get directory info",
            description = "Returns collection of resources containing in directory.",
            parameters = @Parameter(name = "path", description = "Path where resource must be uploaded")
    )
    @ApiResponses({
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
                    description = "Directory does not exists",
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
    })
    @GetMapping
    public List<ResourceInfoResponseDTO> getDirectory(@RequestParam("path") GetDirectoryContentRequestParam request,
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
                            description = "Parent directory doesn't exists",
                            responseCode = "404",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Directory already existed",
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
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceInfoResponseDTO createDirectory(@Valid @RequestParam("path") @NotBlank(message = "Parameter \"path\" must not be blank") String path,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return storageService.createDirectory(userDetails.getId(), path);
    }
}
