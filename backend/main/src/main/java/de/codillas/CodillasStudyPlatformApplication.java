package de.codillas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point. Lives in the base package {@code de.codillas} so component scanning and Spring
 * Modulith pick up every {@code de.codillas.<module>} package.
 *
 * <p>{@code @EnableAsync} powers the asynchronous {@code @ApplicationModuleListener} delivery used
 * by event-fed read models (e.g. gradebook), backed by the JDBC event publication registry.
 */
@SpringBootApplication
@EnableAsync
public class CodillasStudyPlatformApplication {

  static void main(String[] args) {
    SpringApplication.run(CodillasStudyPlatformApplication.class, args);
  }
}
