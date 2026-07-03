package de.codillas;

import org.springframework.modulith.core.ApplicationModules;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Enforces the module boundaries (events + by-id references only) at build time.
 *
 * <p>{@code de.codillas.config} is excluded: it is {@code main}'s application wiring (security,
 * WebSocket, role hierarchy), not a domain module — it may touch module APIs (e.g. chat membership
 * for STOMP subscription authz) the way any composition root does.
 */
@DisplayName("Modulith architecture")
class ModulithArchitectureTest {

  private static final DescribedPredicate<JavaClass> APP_WIRING =
      JavaClass.Predicates.resideInAPackage("de.codillas.config..");

  @Test
  @DisplayName("module boundaries hold — no cross-module internals, no cycles")
  void modulesVerify() {
    ApplicationModules.of(CodillasStudyPlatformApplication.class, APP_WIRING).verify();
  }
}
