package de.codillas.course;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Shared Postgres container for this module's integration tests. Spring Boot's
 * {@code @ServiceConnection} support starts the container and wires its datasource into the
 * environment — no manual {@code @DynamicPropertySource} plumbing.
 */
public abstract class PostgresTestContainer {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");
}
