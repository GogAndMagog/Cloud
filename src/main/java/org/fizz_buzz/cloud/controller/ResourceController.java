package org.fizz_buzz.cloud.controller;

import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.security.CustomUserDetails;
import org.fizz_buzz.cloud.service.S3UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resource")
public class ResourceController {

    private final S3UserService s3UserService;


    @GetMapping
    public ResourceInfoResponseDTO getResource(@RequestParam(name = "path") String path,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {

        return s3UserService.getResource(userDetails.getId(), path);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@RequestParam(name = "path") String path,
                               @AuthenticationPrincipal CustomUserDetails userDetails) {

        s3UserService.deleteResource(userDetails.getId(), path);
    }

    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam(name = "path") String path,
                                                                  Authentication authentication,
                                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {

        StreamingResponseBody streamingResponseBody = s3UserService.downloadResource(userDetails.getId(), path);

        Path entirePath = Paths.get(path);
        String fileName;

        if (path.endsWith("/")){
            fileName = entirePath.getFileName().toString().concat(".zip");
        }
        else {
            fileName = entirePath.getFileName().toString();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(fileName))
                .body(streamingResponseBody);
    }
}
