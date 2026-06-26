package de.codillas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point. Lives in the base package {@code de.codillas} so component scanning and Spring
 * Modulith pick up every {@code de.codillas.<module>} package.
 *
 * <p>{@code @EnableAsync} powers the asynchronous {@code @ApplicationModuleListener} delivery used
 * by event-fed read models (e.g. gradebook), backed by the JDBC event publication registry.
 *
 * <p>{@code @EnableScheduling} powers {@code @Scheduled} jobs (e.g. homework's deadline reminder).
 * The jobs themselves are guarded with {@code @Profile("!integration-test")} so the scheduler never
 * fires during the deterministic IT suite.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CodillasStudyPlatformApplication {

  static void main(String[] args) {
    SpringApplication.run(CodillasStudyPlatformApplication.class, args);
  }
}
