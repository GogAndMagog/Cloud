package org.fizz_buzz.cloud.controller;

import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.security.CustomUserDetails;
import org.fizz_buzz.cloud.service.S3UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resource")
public class ResourceController {

    private final S3UserService s3UserService;


    @GetMapping
    public ResourceInfoResponseDTO getResource(@RequestParam(name = "path") String path,
                                               Authentication authentication){

        var userId = ((CustomUserDetails) authentication.getPrincipal()).getId();

        return s3UserService.getResource(userId, path);
    }
}
