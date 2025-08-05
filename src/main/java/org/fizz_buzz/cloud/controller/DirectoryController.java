package org.fizz_buzz.cloud.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.security.CustomUserDetails;
import org.fizz_buzz.cloud.service.S3UserService;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("api/v1/directory")
@RequiredArgsConstructor
public class DirectoryController {

    private final S3UserService s3UserService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    }

    @GetMapping
    public List<ResourceInfoResponseDTO> getDirectory(@RequestParam(name = "path")
                                                      String path,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {

        return s3UserService.getDirectory(userDetails.getId(), path);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceInfoResponseDTO createDirectory(@Valid
                                                   @RequestParam(name = "path")
                                                   @NotBlank(message = "Parameter \"path\" must not be blank") String path,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {

        return s3UserService.createDirectory(userDetails.getId(), path);
    }
}
