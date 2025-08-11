package org.fizz_buzz.cloud.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.response.ResourceInfoResponseDTO;
import org.fizz_buzz.cloud.security.CustomUserDetails;
import org.fizz_buzz.cloud.service.S3UserService;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resource")
public class ResourceController {

    private final S3UserService s3UserService;


    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    }

    @GetMapping
    public ResourceInfoResponseDTO getResource(@Valid
                                               @RequestParam(name = "path")
                                               @NotBlank(message = "Parameter \"path\" must not be blank") String path,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {

        return s3UserService.getResource(userDetails.getId(), path);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@Valid
                               @RequestParam(name = "path")
                               @NotBlank(message = "Parameter \"path\" must not be blank") String path,
                               @AuthenticationPrincipal CustomUserDetails userDetails) {

        s3UserService.deleteResource(userDetails.getId(), path);
    }

    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadResource(@Valid
                                                                  @RequestParam(name = "path")
                                                                  @NotBlank(message = "Parameter \"path\" must not be blank") String path,
                                                                  @AuthenticationPrincipal CustomUserDetails userDetails) throws UnsupportedEncodingException {

        StreamingResponseBody streamingResponseBody = s3UserService.downloadResource(userDetails.getId(), path);

        Path entirePath = Paths.get(path);
        String fileName;

        if (path.endsWith("/")) {
            fileName = entirePath.getFileName().toString().concat(".zip");
        } else {
            fileName = entirePath.getFileName().toString();
        }

        // needed to support other languages, not only English
        String encodedFileName =  URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''%s".formatted(encodedFileName))
                .body(streamingResponseBody);
    }

    @GetMapping("/move")
    public ResourceInfoResponseDTO move(@Valid
                                        @RequestParam(name = "from")
                                        @NotBlank(message = "Parameter \"from\" must not be blank") String from,
                                        @Valid
                                        @RequestParam(name = "to")
                                        @NotBlank(message = "Parameter \"to\" must not be blank") String to,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {

        return s3UserService.moveResource(userDetails.getId(), from, to);
    }

    @GetMapping("/search")
    public List<ResourceInfoResponseDTO> search(@Valid
                                                @RequestParam(name = "query")
                                                @NotBlank(message = "Parameter \"query\" must not be blank")
                                                String query,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {

        return s3UserService.searchResource(userDetails.getId(), query);
    }

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceInfoResponseDTO> upload(@RequestParam(name = "path") String path,
                                                @RequestParam(name = "object") MultipartFile[] files,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {

        return s3UserService.upload(userDetails.getId(), path, files);
    }
}
