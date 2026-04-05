package com.example.ADD.project.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemService {

    private final ApplicationContext context;

    public void shutdownApp() {
        // 별도의 쓰레드에서 종료를 실행하여 API 응답이 정상적으로 반환될 수 있도록 함
        Thread thread = new Thread(() -> SpringApplication.exit(context, () -> 0));
        thread.start();
    }
}