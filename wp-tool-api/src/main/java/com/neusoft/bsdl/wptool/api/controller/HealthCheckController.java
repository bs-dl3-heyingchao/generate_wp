package com.neusoft.bsdl.wptool.api.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neusoft.bsdl.wptool.api.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class HealthCheckController {

    @GetMapping("/health-check")
    public ApiResponse<Map<String, String>> healthCheck() {
        return ApiResponse.success(Map.of("status", "UP"));
    }
}