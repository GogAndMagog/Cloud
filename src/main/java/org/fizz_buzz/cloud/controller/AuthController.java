package org.fizz_buzz.cloud.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.fizz_buzz.cloud.dto.UserDTO;
import org.fizz_buzz.cloud.dto.view.UserViews;
import org.fizz_buzz.cloud.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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


    @PostMapping(value = "/sign_up", produces = MediaType.APPLICATION_JSON_VALUE)
    @JsonView(UserViews.Response.class)
    public ResponseEntity<UserDTO> signUp(@Valid @RequestBody UserDTO request) {

        var response = authService.signUp(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/sign_in", produces = MediaType.APPLICATION_JSON_VALUE)
    @JsonView(UserViews.Response.class)
    public ResponseEntity<UserDTO> signIn(@Valid @RequestBody UserDTO request,
                                          HttpSession session) {

        var response = authService.signIn(request, session);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<String> getTest() {
        return ResponseEntity.ok().body("Test");
    }
}
