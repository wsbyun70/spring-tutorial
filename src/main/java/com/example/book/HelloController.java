package com.example.book;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    @GetMapping("/hello/{name}")
    public String helloName(@PathVariable String name) {
        return "Hello, " + name + "!";
    }

    @GetMapping("/greet")
    public String greet(
            @RequestParam(defaultValue = "Guest") String name,
            @RequestParam(defaultValue = "1") int times) {
        return "Hi " + name + "! (" + times + " times)";
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> result = new HashMap<>();
        result.put("app", "book-tutorial");
        result.put("version", "0.0.1-SNAPSHOT");
        result.put("javaVersion", System.getProperty("java.version"));
        return result;
    }
}
