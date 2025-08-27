package org.fizz_buzz.cloud.controller;

import jakarta.validation.constraints.NotNull;
import org.fizz_buzz.cloud.dto.response.UserResponseDTO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("api/v1/user/")
public class UserController {

    @GetMapping("/me")
    public UserResponseDTO getMe(@NotNull Authentication authentication){

        return new UserResponseDTO(authentication.getName());
    }
}
