package com.example.ADD.project.backend.logger;

import jakarta.servlet.MultipartConfigElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MultipartRuntimeLogger {

  public MultipartRuntimeLogger(MultipartConfigElement multipartConfigElement) {
    log.info("multipart maxFileSize={}", multipartConfigElement.getMaxFileSize());
    log.info("multipart maxRequestSize={}", multipartConfigElement.getMaxRequestSize());
    log.info("multipart fileSizeThreshold={}", multipartConfigElement.getFileSizeThreshold());
  }
}
