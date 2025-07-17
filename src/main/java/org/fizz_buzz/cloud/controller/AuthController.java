package org.fizz_buzz.cloud.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.request.UserRequestDTO;
import org.fizz_buzz.cloud.dto.response.UserResponseDTO;
import org.fizz_buzz.cloud.service.AuthService;
import org.fizz_buzz.cloud.service.S3UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final S3UserService s3UserService;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();


    @PostMapping(value = "/sign-up")
    public ResponseEntity<UserResponseDTO> signUp(@Valid @RequestBody UserRequestDTO request) {

        var user = authService.signUp(request);
        s3UserService.createUserDirectory(user.getId());

        return new ResponseEntity<>(new UserResponseDTO(user.getName()), HttpStatus.CREATED);
    }

    @PostMapping(value = "/sign-in")
    public ResponseEntity<UserResponseDTO> signIn(@Valid @RequestBody UserRequestDTO request,
                                                  HttpServletRequest httpServletRequest,
                                                  HttpServletResponse httpServletResponse) {

        var response = authService.signIn(request, httpServletRequest, httpServletResponse);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = "/sign-out")
    public ResponseEntity<Void> signOut(Authentication authentication,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {

        this.logoutHandler.logout(request, response, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping
    public ResponseEntity<String> getTest() {
        return ResponseEntity.ok().body("Test");
    }
}
