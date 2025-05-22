package org.fizz_buzz.cloud.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//needed to test authentication
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping
    public String getTestData() {

        return "test data";
    }
}
