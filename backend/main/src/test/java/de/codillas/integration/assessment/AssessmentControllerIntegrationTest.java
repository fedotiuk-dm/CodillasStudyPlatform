package de.codillas.integration.assessment;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AssessmentController (integration)")
class AssessmentControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName("full flow: build a test, publish, attempt it and auto-grade a correct answer")
  void fullAssessmentFlow() throws Exception {
    UUID teacher = UUID.randomUUID();
    UUID student = UUID.randomUUID();

    String test =
        mockMvc
            .perform(
                post("/api/tests")
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Quiz 1\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID testId = UUID.fromString(JsonPath.read(test, "$.id"));

    String question =
        mockMvc
            .perform(
                post("/api/tests/{testId}/questions", testId)
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"type\":\"SINGLE_CHOICE\",\"prompt\":\"2+2?\",\"points\":5,"
                            + "\"options\":[{\"text\":\"4\",\"correct\":true},"
                            + "{\"text\":\"5\",\"correct\":false}]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.options.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID questionId = UUID.fromString(JsonPath.read(question, "$.id"));
    UUID correctOptionId = UUID.fromString(JsonPath.read(question, "$.options[0].id"));

    mockMvc
        .perform(post("/api/tests/{testId}/publish", testId).with(as(teacher, "TEACHER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));

    String attempt =
        mockMvc
            .perform(post("/api/tests/{testId}/attempts", testId).with(as(student, "STUDENT")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID attemptId = UUID.fromString(JsonPath.read(attempt, "$.id"));

    mockMvc
        .perform(
            put("/api/attempts/{attemptId}/answers", attemptId)
                .with(as(student, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"questionId\":\"%s\",\"selectedOptionIds\":[\"%s\"]}"
                        .formatted(questionId, correctOptionId)))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/attempts/{attemptId}/submit", attemptId).with(as(student, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("GRADED"))
        .andExpect(jsonPath("$.score").value(5));
  }

  @Test
  @DisplayName("a student cannot create a test (403)")
  void createTest_asStudent_returns403() throws Exception {
    mockMvc
        .perform(
            post("/api/tests")
                .with(as(UUID.randomUUID(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("starting an attempt again resumes the same attempt instead of creating a new one")
  void startAttempt_twice_resumesSameAttempt() throws Exception {
    UUID teacher = UUID.randomUUID();
    UUID student = UUID.randomUUID();

    String test =
        mockMvc
            .perform(
                post("/api/tests")
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Quiz 2\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID testId = UUID.fromString(JsonPath.read(test, "$.id"));
    mockMvc
        .perform(post("/api/tests/{testId}/publish", testId).with(as(teacher, "TEACHER")))
        .andExpect(status().isOk());

    String first =
        mockMvc
            .perform(post("/api/tests/{testId}/attempts", testId).with(as(student, "STUDENT")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID firstAttemptId = UUID.fromString(JsonPath.read(first, "$.id"));

    // Reopening the same test resumes the existing attempt (same id), it does not create a new one.
    mockMvc
        .perform(post("/api/tests/{testId}/attempts", testId).with(as(student, "STUDENT")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(firstAttemptId.toString()))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }
}
