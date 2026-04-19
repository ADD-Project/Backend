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
        // 비동기로 종료 프로세스를 실행하여, 프론트엔드에 먼저 성공 응답(200 OK)을 보냅니다.
        new Thread(() -> {
            try {
                // 1. 프론트엔드가 응답을 받을 수 있도록 1~2초 대기
                Thread.sleep(1500);

                // 2. 윈도우 환경에서 크롬 브라우저 프로세스 강제 종료
                // 주의: 이 명령어는 실행 중인 모든 크롬 창을 닫습니다. (키오스크 전용 PC에 적합)
                Runtime.getRuntime().exec("taskkill /IM chrome.exe /F");

                // 3. 약간의 대기 후 Java 애플리케이션(백엔드) 종료
                Thread.sleep(500);
                System.exit(0);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}