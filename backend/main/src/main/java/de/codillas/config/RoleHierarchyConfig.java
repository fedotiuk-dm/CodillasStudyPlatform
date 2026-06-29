package de.codillas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

/**
 * Role gradation: {@code ADMIN > TEACHER > STUDENT}. An ADMIN automatically satisfies any
 * {@code @RequiresTeacher} / {@code @RequiresStudent} check, and a TEACHER satisfies
 * {@code @RequiresStudent} — so higher roles never need lower roles granted explicitly in Keycloak.
 *
 * <p>Spring Security applies a published {@link RoleHierarchy} bean to method security
 * automatically. Keeps the {@code ROLE_} prefix (this app maps realm roles to {@code ROLE_*} via
 * {@code authorities-prefix}), so {@code hasRole('TEACHER')} checks {@code ROLE_TEACHER}. Active in
 * all profiles, including integration tests.
 *
 * <p>NOT a {@code @UtilityClass}: Spring must instantiate this {@code @Configuration} bean (even
 * with a {@code static @Bean}), and Lombok's generated constructor throws — which breaks context
 * startup. The plain private constructor below silences the S1118 warning without that trap.
 */
@Configuration(proxyBeanMethods = false)
public class RoleHierarchyConfig {

  private RoleHierarchyConfig() {} // only the static @Bean factory below — never used by callers.

  @Bean
  static RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withDefaultRolePrefix()
        .role("ADMIN")
        .implies("TEACHER")
        .role("TEACHER")
        .implies("STUDENT")
        .build();
  }
}
