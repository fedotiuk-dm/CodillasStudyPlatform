package de.codillas.integration.homework;

import static org.assertj.core.api.Assertions.assertThat;
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

import de.codillas.homework.domain.model.Grade;
import de.codillas.homework.domain.repository.GradeRepository;
import de.codillas.integration.BaseIntegrationTest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves the grade upsert against a real Postgres: re-grading the same submission must update the
 * single existing row, not attempt a second INSERT that the {@code submission_id} unique constraint
 * would reject with a 500.
 */
@DisplayName("Grade re-grade upsert (integration)")
class GradeReGradeIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private GradeRepository grades;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName(
      "re-grading with a new score updates the single grade row (no unique-constraint 500)")
  void reGradeUpdatesSingleRow() throws Exception {
    UUID teacher = UUID.randomUUID();
    UUID student = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();

    String created =
        mockMvc
            .perform(
                post("/api/assignments")
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"groupId\":\"%s\",\"title\":\"HW1\"}".formatted(groupId)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID assignmentId = UUID.fromString(JsonPath.read(created, "$.id"));

    mockMvc
        .perform(post("/api/assignments/{id}/publish", assignmentId).with(as(teacher, "TEACHER")))
        .andExpect(status().isOk());

    String submission =
        mockMvc
            .perform(
                post("/api/assignments/{id}/submissions", assignmentId)
                    .with(as(student, "STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"my answer\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID submissionId = UUID.fromString(JsonPath.read(submission, "$.id"));

    mockMvc
        .perform(put("/api/submissions/{id}/submit", submissionId).with(as(student, "STUDENT")))
        .andExpect(status().isOk());

    // First grade.
    mockMvc
        .perform(
            post("/api/submissions/{id}/grade", submissionId)
                .with(as(teacher, "TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":60}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.score").value(60));

    // Re-grade with a different score: must succeed (upsert), not blow up on the unique constraint.
    mockMvc
        .perform(
            post("/api/submissions/{id}/grade", submissionId)
                .with(as(teacher, "TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":95}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.score").value(95));

    // Exactly one persisted grade row for the submission, carrying the updated score.
    var rows =
        grades.findAll().stream()
            .filter(g -> submissionId.equals(g.getSubmissionId()))
            .map(Grade::getScore)
            .toList();
    assertThat(rows).containsExactly(95);
  }
}
