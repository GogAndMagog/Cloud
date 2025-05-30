package org.fizz_buzz.cloud.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.fizz_buzz.cloud.dto.request.RequestSignInDTO;
import org.fizz_buzz.cloud.dto.request.RequestSignUpDTO;
import org.fizz_buzz.cloud.dto.response.ResponseSignInDTO;
import org.fizz_buzz.cloud.dto.response.ResponseSignUpDTO;
import org.fizz_buzz.cloud.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/auth", consumes = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    @Autowired
    private AuthService authService;


    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping(value = "/sign_up", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseSignUpDTO> signUp(@Valid @RequestBody RequestSignUpDTO request){

        var responseDto = authService.signUp(request);

        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping(value = "/sign_in", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseSignInDTO> signIn(@Valid @RequestBody RequestSignInDTO request,
                                                    HttpServletRequest httpRequest){

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
        Authentication authenticationResponse =
                this.authenticationManager.authenticate(authenticationRequest);

        if (authenticationResponse != null &&
            authenticationResponse.isAuthenticated()) {
            ResponseSignInDTO responseDto = new ResponseSignInDTO(authenticationResponse.getName());

            SecurityContextHolder.getContext().setAuthentication(authenticationResponse);
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
        }
        else {
            throw new AuthenticationCredentialsNotFoundException("Credentials not found!");
        }
    }

    @GetMapping
    public ResponseEntity<String> getTest()
    {
        return ResponseEntity.ok().body("Test");
    }
}
