package com.example.ADD.project.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 브라우저에서 /images/... 로 요청이 오면
        // 애플리케이션(jar)이 실행되는 현재 디렉토리(./)의 images 폴더 안에서 파일을 찾아서 반환합니다.
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:./images/");
    }
}