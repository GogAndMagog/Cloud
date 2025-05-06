package org.fizz_buzz.cloud.controller;

import org.fizz_buzz.cloud.dto.request.RequestSignUpDTO;
import org.fizz_buzz.cloud.dto.response.ResponseSignUpDTO;
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
    public ResponseEntity<ResponseSignUpDTO> signUp(@RequestBody RequestSignUpDTO request){

        var responseDto = authService.signUp(request);

        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<String> getTest()
    {
        return ResponseEntity.ok().body("Test");
    }
}
