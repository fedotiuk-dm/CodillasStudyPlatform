package de.codillas.integration.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.integration.BaseIntegrationTest;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserController (integration)")
class UserControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor asUser(UUID userId) {
    return jwt().jwt(j -> j.subject(userId.toString()));
  }

  @Test
  @DisplayName("PUT /api/users/me upserts the profile and returns 200 with it")
  void updateMyProfile_upsertsAndReturns200() throws Exception {
    UUID userId = UUID.randomUUID();
    mockMvc
        .perform(
            put("/api/users/me")
                .with(asUser(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Ada\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.displayName").value("Ada"));
  }

  @Test
  @DisplayName("GET /api/users/me returns the profile created by a prior PUT")
  void getMyProfile_afterUpsert_returns200() throws Exception {
    UUID userId = UUID.randomUUID();
    var user = asUser(userId);
    mockMvc
        .perform(
            put("/api/users/me")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Bob\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/users/me").with(user))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Bob"));
  }

  @Test
  @DisplayName("GET /api/users/me surfaces the caller's granted roles")
  void getMyProfile_surfacesRoles() throws Exception {
    UUID userId = UUID.randomUUID();
    mockMvc
        .perform(
            get("/api/users/me")
                .with(
                    jwt()
                        .jwt(j -> j.subject(userId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_TEACHER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles").value(Matchers.contains("TEACHER")));
  }

  @Test
  @DisplayName("GET /api/users/me provisions a profile on first access (no 404)")
  void getMyProfile_whenNoProfile_provisions() throws Exception {
    UUID userId = UUID.randomUUID();
    mockMvc
        .perform(get("/api/users/me").with(asUser(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        // No name claim on the test token → display name falls back to the subject.
        .andExpect(jsonPath("$.displayName").value(userId.toString()));
  }
}
