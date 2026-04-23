package com.example.ADD.project.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @GetMapping("/pages")
    public List<String> getPageMediaFiles() {
        // 실제 app.jar 파일이 실행되는 위치 기준의 ./images/pages 폴더를 바라봅니다.
        File folder = new File("./images/pages");

        // 폴더가 없거나 디렉토리가 아니면 빈 리스트 반환
        if (!folder.exists() || !folder.isDirectory()) {
            return Collections.emptyList();
        }

        // 이미지 및 동영상 확장자만 필터링
        File[] files = folder.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.matches(".*\\.(png|jpg|jpeg|gif|webp|bmp|mp4|webm|ogg|mov)$");
        });

        if (files == null) return Collections.emptyList();

        // 파일명에 포함된 숫자를 추출하여 오름차순 정렬 후 파일명 목록 반환
        return Arrays.stream(files)
                .map(File::getName)
                .sorted((f1, f2) -> {
                    int num1 = extractNumber(f1);
                    int num2 = extractNumber(f2);
                    return Integer.compare(num1, num2);
                })
                .collect(Collectors.toList());
    }

    // 파일명에서 숫자만 추출하는 헬퍼 메서드 (예: "1.jpg" -> 1, "10_intro.mp4" -> 10)
    private int extractNumber(String filename) {
        String numStr = filename.replaceAll("[^0-9]", "");
        return numStr.isEmpty() ? 0 : Integer.parseInt(numStr);
    }
}