package de.codillas.integration.homework;

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

@DisplayName("HomeworkController (integration)")
class HomeworkControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName("full flow: teacher creates+publishes, student submits, teacher grades")
  void fullHomeworkFlow() throws Exception {
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
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID assignmentId = UUID.fromString(JsonPath.read(created, "$.id"));

    mockMvc
        .perform(post("/api/assignments/{id}/publish", assignmentId).with(as(teacher, "TEACHER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));

    String submission =
        mockMvc
            .perform(
                post("/api/assignments/{id}/submissions", assignmentId)
                    .with(as(student, "STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"my answer\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.studentId").value(student.toString()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID submissionId = UUID.fromString(JsonPath.read(submission, "$.id"));

    mockMvc
        .perform(put("/api/submissions/{id}/submit", submissionId).with(as(student, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));

    mockMvc
        .perform(
            post("/api/submissions/{id}/grade", submissionId)
                .with(as(teacher, "TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":95}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.score").value(95))
        .andExpect(jsonPath("$.gradedBy").value(teacher.toString()));
  }

  @Test
  @DisplayName("a student cannot create an assignment (403)")
  void createAssignment_asStudent_returns403() throws Exception {
    mockMvc
        .perform(
            post("/api/assignments")
                .with(as(UUID.randomUUID(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupId\":\"%s\",\"title\":\"HW1\"}".formatted(UUID.randomUUID())))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("grading a submission that was never submitted is a conflict (409)")
  void grade_draftSubmission_returns409() throws Exception {
    UUID teacher = UUID.randomUUID();
    UUID student = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();

    String submission =
        mockMvc
            .perform(
                post("/api/assignments/{id}/submissions", assignmentId)
                    .with(as(student, "STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"draft\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID submissionId = UUID.fromString(JsonPath.read(submission, "$.id"));

    mockMvc
        .perform(
            post("/api/submissions/{id}/grade", submissionId)
                .with(as(teacher, "TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":50}"))
        .andExpect(status().isConflict());
  }
}
