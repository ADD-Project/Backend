package com.example.ADD.project.backend.controller;

import com.example.ADD.project.backend.dto.ApiResponse;
import com.example.ADD.project.backend.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @PostMapping("/system/shutdown")
    public ApiResponse<String> shutdownSystem() {
        systemService.shutdownApp();
        return ApiResponse.success("200", "서버 종료가 요청되었습니다.", null);
    }
}