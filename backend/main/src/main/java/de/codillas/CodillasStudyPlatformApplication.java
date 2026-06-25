package de.codillas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Lives in the base package {@code de.codillas} so component scanning and Spring
 * Modulith pick up every {@code de.codillas.<module>} package.
 */
@SpringBootApplication
public class CodillasStudyPlatformApplication {

  static void main(String[] args) {
    SpringApplication.run(CodillasStudyPlatformApplication.class, args);
  }
}
