package de.codillas.integration.assessment;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.integration.BaseIntegrationTest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Assessment attempt object-level authorization (integration)")
class AttemptAuthorizationIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName("student B cannot read or answer student A's attempt (404); A can read it")
  void otherStudentCannotTouchAttempt() throws Exception {
    UUID teacher = UUID.randomUUID();
    UUID studentA = UUID.randomUUID();
    UUID studentB = UUID.randomUUID();

    String test =
        mockMvc
            .perform(
                post("/api/tests")
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"T\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID testId = UUID.fromString(JsonPath.read(test, "$.id"));

    mockMvc
        .perform(post("/api/tests/{id}/publish", testId).with(as(teacher, "TEACHER")))
        .andExpect(status().isOk());

    String attempt =
        mockMvc
            .perform(post("/api/tests/{id}/attempts", testId).with(as(studentA, "STUDENT")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID attemptId = UUID.fromString(JsonPath.read(attempt, "$.id"));

    mockMvc
        .perform(get("/api/attempts/{id}", attemptId).with(as(studentB, "STUDENT")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            put("/api/attempts/{id}/answers", attemptId)
                .with(as(studentB, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\":\"%s\"}".formatted(UUID.randomUUID())))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/attempts/{id}", attemptId).with(as(studentA, "STUDENT")))
        .andExpect(status().isOk());
  }
}
