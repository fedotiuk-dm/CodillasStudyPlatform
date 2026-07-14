package de.codillas.integration.gradebook;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.integration.BaseIntegrationTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Gradebook object-level authorization (integration)")
class GradebookAuthorizationIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName("a student may read only their own gradebook; another's is 404; staff reads any")
  void studentGradebookIsSelfOrStaff() throws Exception {
    UUID studentA = UUID.randomUUID();
    UUID studentB = UUID.randomUUID();

    mockMvc
        .perform(get("/api/gradebook/students/{id}", studentA).with(as(studentA, "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/gradebook/students/{id}", studentA).with(as(studentB, "STUDENT")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/gradebook/students/{id}", studentA).with(as(UUID.randomUUID(), "TEACHER")))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("a non-member cannot read a group gradebook (404); staff can")
  void groupGradebookIsMemberOrStaff() throws Exception {
    UUID groupId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/gradebook/groups/{id}", groupId).with(as(UUID.randomUUID(), "STUDENT")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/gradebook/groups/{id}", groupId).with(as(UUID.randomUUID(), "TEACHER")))
        .andExpect(status().isOk());
  }
}
