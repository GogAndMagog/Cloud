package org.fizz_buzz.cloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.MessageDTO;
import org.fizz_buzz.cloud.dto.request.UserRequestDTO;
import org.fizz_buzz.cloud.dto.response.UserResponseDTO;
import org.fizz_buzz.cloud.service.AuthService;
import org.fizz_buzz.cloud.service.S3UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Auth management",
        description = "Authentication and authorization created in RPC-architecture style"
)
@RestController
@RequestMapping(value = "/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final S3UserService s3UserService;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();


    @Operation(
            summary = "Registration method",
            description = "Registration method takes JSON with credentials and register user in system.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User credentials",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserRequestDTO.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            description = "Registration success",
                            responseCode = "201",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UserResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Validation error",
                            responseCode = "400",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "User with that name already exists",
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
    @PostMapping(value = "/sign-up")
    public ResponseEntity<UserResponseDTO> signUp(@Valid @RequestBody UserRequestDTO request) {

        var user = authService.signUp(request);
        s3UserService.createUserDirectory(user.getId());

        return new ResponseEntity<>(new UserResponseDTO(user.getName()), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Login method",
            description = "Login method takes JSON with credentials and creates session for user.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User credentials",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserRequestDTO.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            description = "Registration success",
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UserResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Validation error",
                            responseCode = "400",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageDTO.class)
                            )
                    ),
                    @ApiResponse(
                            description = "Wrong credentials",
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
    @PostMapping(value = "/sign-in")
    public ResponseEntity<UserResponseDTO> signIn(@Valid @RequestBody UserRequestDTO request,
                                                  HttpServletRequest httpServletRequest,
                                                  HttpServletResponse httpServletResponse) {

        var response = authService.signIn(request, httpServletRequest, httpServletResponse);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Logout method",
            description = "Logout method analyze session to identify user.",
            responses = {
                    @ApiResponse(
                            description = "Logout success",
                            responseCode = "204"),
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
    @PostMapping(value = "/sign-out")
    public ResponseEntity<Void> signOut(Authentication authentication,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {

        this.logoutHandler.logout(request, response, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
