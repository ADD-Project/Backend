package com.example.ADD.project.backend.logger;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupLogger {

  public StartupLogger(
          @Value("${spring.profiles.active:}") String activeProfile,
          @Value("${spring.datasource.url}") String datasourceUrl,
          @Value("${spring.jpa.hibernate.ddl-auto:}") String ddlAuto
  ) {
    log.info("activeProfile={}", activeProfile);
    log.info("datasourceUrl={}", datasourceUrl);
    log.info("ddlAuto={}", ddlAuto);
  }
}