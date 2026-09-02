package com.nexo.backend;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class HolaController {
    @GetMapping("/api/hello")
    public String helloWorld() {
        return "Hello, World from spring boot!";
    }
}
