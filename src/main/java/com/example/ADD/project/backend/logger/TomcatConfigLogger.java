package com.example.ADD.project.backend.logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TomcatConfigLogger {

  public TomcatConfigLogger(
          @Value("${server.tomcat.max-http-form-post-size}") String maxHttpFormPostSize,
          @Value("${server.tomcat.max-swallow-size}") String maxSwallowSize,
          @Value("${server.tomcat.max-part-count}") String maxPartCount
  ) {
    log.info("tomcat max-http-form-post-size={}", maxHttpFormPostSize);
    log.info("tomcat max-swallow-size={}", maxSwallowSize);
    log.info("tomcat max-part-count={}", maxPartCount);
  }
}