package com.example.ADD.project.backend.config;

import com.example.ADD.project.backend.interceptor.AdminCheckInterceptor;
import com.example.ADD.project.backend.interceptor.MemberCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 관리자 권한 체크 인터셉터 (예: /admin/** 경로는 모두 관리자만 접근 가능)
        registry.addInterceptor(new AdminCheckInterceptor())
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login"); // 로그인 API는 제외
    }
}