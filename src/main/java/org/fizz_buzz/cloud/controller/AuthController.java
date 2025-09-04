package org.fizz_buzz.cloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.response.ErrorMessageResponseDto;
import org.fizz_buzz.cloud.dto.request.UserRequestDTO;
import org.fizz_buzz.cloud.dto.response.UserResponseDTO;
import org.fizz_buzz.cloud.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Auth management",
        description = "Authentication and authorization created in RPC naming style"
)
@RestController
@RequestMapping(value = "/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LogoutHandler logoutHandler;


    @Operation(
            summary = "Registration method",
            description = "Registration method takes JSON with credentials and register user in system.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User credentials",
                    content = @Content(schema = @Schema(implementation = UserRequestDTO.class))
            ),
            responses = {
                    @ApiResponse(
                            description = "Registration success",
                            responseCode = "201",
                            content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
                    ),
                    @ApiResponse(
                            description = "Validation error",
                            responseCode = "400",
                            content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
                    ),
                    @ApiResponse(
                            description = "User with that name already exists",
                            responseCode = "409",
                            content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
                    )
            }
    )
    @PostMapping(value = "/sign-up")
    public ResponseEntity<UserResponseDTO> signUp(@RequestBody @Valid UserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    @Operation(
            summary = "Login method",
            description = "Login method takes JSON with credentials and creates session for user.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User credentials",
                    content = @Content(schema = @Schema(implementation = UserRequestDTO.class))
            ),
            responses = {
                    @ApiResponse(
                            description = "Registration success",
                            responseCode = "200",
                            content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
                    ),
                    @ApiResponse(
                            description = "Validation error",
                            responseCode = "400",
                            content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
                    ),
                    @ApiResponse(
                            description = "Wrong credentials",
                            responseCode = "401",
                            content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
                    )
            }
    )
    @PostMapping(value = "/sign-in")
    public ResponseEntity<UserResponseDTO> signIn(@RequestBody @Valid UserRequestDTO request) {
        return ResponseEntity.ok(authService.signIn(request));
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
                            content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(schema = @Schema(implementation = ErrorMessageResponseDto.class))
                    )
            }
    )
    @PostMapping(value = "/sign-out")
    public ResponseEntity<Void> signOut(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        this.logoutHandler.logout(request, response, authentication);

        return ResponseEntity.noContent().build();
    }
}
