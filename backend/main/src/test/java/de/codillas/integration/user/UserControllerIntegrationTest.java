package de.codillas.integration.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.integration.BaseIntegrationTest;

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
  @DisplayName("GET /api/users/me returns 404 when the user has no profile yet")
  void getMyProfile_whenNoProfile_returns404() throws Exception {
    mockMvc
        .perform(get("/api/users/me").with(asUser(UUID.randomUUID())))
        .andExpect(status().isNotFound());
  }
}
