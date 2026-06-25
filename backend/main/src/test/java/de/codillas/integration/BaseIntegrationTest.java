package de.codillas.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import de.codillas.CodillasStudyPlatformApplication;

/**
 * Base for integration tests. Boots the real application against a Testcontainers Postgres with the
 * production Liquibase migrations (profile {@code integration-test}, {@code ddl-auto: validate}).
 *
 * <p>Lives in {@code main} so the full Spring context (every module's beans) is present —
 * autowiring resolves cleanly, no IDE false-positives and no {@code @SuppressWarnings}.
 */
// MOCK (not NONE): a servlet web context is needed so Spring Security's HttpSecurity bean exists
// for SecurityConfig. Add @AutoConfigureMockMvc in subclasses that drive HTTP endpoints.
@SpringBootTest(
    classes = CodillasStudyPlatformApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public abstract class BaseIntegrationTest extends SharedTestContainers {}
