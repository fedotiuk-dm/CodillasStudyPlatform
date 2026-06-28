package de.codillas.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CurrentUser staff / role predicates")
class CurrentUserTest {

  private static CurrentUser withRoles(String... roles) {
    return new CurrentUser() {
      @Override
      public UUID id() {
        return UUID.randomUUID();
      }

      @Override
      public String displayName() {
        return "test";
      }

      @Override
      public Set<String> roles() {
        return Set.of(roles);
      }

      @Override
      public String email() {
        return null;
      }
    };
  }

  @Test
  @DisplayName("hasRole matches the granted role name")
  void hasRole() {
    assertThat(withRoles("TEACHER").hasRole(Role.TEACHER)).isTrue();
    assertThat(withRoles("STUDENT").hasRole(Role.TEACHER)).isFalse();
  }

  @Test
  @DisplayName("isStaff is true for TEACHER or ADMIN, false for STUDENT or anonymous")
  void isStaff() {
    assertThat(withRoles("ADMIN").isStaff()).isTrue();
    assertThat(withRoles("TEACHER").isStaff()).isTrue();
    assertThat(withRoles("STUDENT").isStaff()).isFalse();
    assertThat(withRoles().isStaff()).isFalse();
  }
}
