package de.codillas.integration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Shared Postgres container for integration tests. Spring Boot's {@code @ServiceConnection} starts
 * it and wires the datasource — one container reused across cached contexts.
 */
public abstract class SharedTestContainers {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");
}
