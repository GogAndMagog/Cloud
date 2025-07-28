package org.fizz_buzz.cloud.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.security.CustomUserDetails;
import org.fizz_buzz.cloud.service.S3UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resource")
public class ResourceController {

    private final S3UserService s3UserService;


    @GetMapping
    public ResourceInfoResponseDTO getResource(@RequestParam(name = "path") String path,
                                               Authentication authentication) {

        var userId = ((CustomUserDetails) authentication.getPrincipal()).getId();

        return s3UserService.getResource(userId, path);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@RequestParam(name = "path") String path,
                               Authentication authentication) {

        var userId = ((CustomUserDetails) authentication.getPrincipal()).getId();

        s3UserService.deleteResource(userId, path);
    }

    @GetMapping(value = "/download",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam(name = "path") String path,
                                                                  Authentication authentication) {

        var userId = ((CustomUserDetails) authentication.getPrincipal()).getId();

        StreamingResponseBody responseBody = outputStream -> {
            try (var resource = s3UserService.downloadResource(userId, path)) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = resource.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(path))
                .body(responseBody);
    }
}
