package de.codillas.integration.homework;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@DisplayName("Homework object-level authorization (integration)")
class HomeworkAuthorizationIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName("student B cannot submit student A's submission (404, not 403); A still can")
  void otherStudentCannotTouchSubmission() throws Exception {
    UUID studentA = UUID.randomUUID();
    UUID studentB = UUID.randomUUID();
    UUID assignmentId = createAssignment(UUID.randomUUID());

    String created =
        mockMvc
            .perform(
                post("/api/assignments/{id}/submissions", assignmentId)
                    .with(as(studentA, "STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"A's answer\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID submissionId = UUID.fromString(JsonPath.read(created, "$.id"));

    mockMvc
        .perform(put("/api/submissions/{id}/submit", submissionId).with(as(studentB, "STUDENT")))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(put("/api/submissions/{id}/submit", submissionId).with(as(studentA, "STUDENT")))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("a student lists only their own submissions; a teacher lists everyone's")
  void studentListsOnlyOwnSubmissions() throws Exception {
    UUID studentA = UUID.randomUUID();
    UUID studentB = UUID.randomUUID();
    UUID teacher = UUID.randomUUID();
    UUID assignmentId = createAssignment(teacher);

    for (UUID student : new UUID[] {studentA, studentB}) {
      mockMvc
          .perform(
              post("/api/assignments/{id}/submissions", assignmentId)
                  .with(as(student, "STUDENT"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"content\":\"answer\"}"))
          .andExpect(status().isCreated());
    }

    mockMvc
        .perform(
            get("/api/assignments/{id}/submissions", assignmentId).with(as(studentA, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].studentId").value(studentA.toString()));

    mockMvc
        .perform(
            get("/api/assignments/{id}/submissions", assignmentId).with(as(teacher, "TEACHER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("staff (teacher) may grade any student's submission")
  void teacherMayGradeAnySubmission() throws Exception {
    UUID studentA = UUID.randomUUID();
    UUID teacher = UUID.randomUUID();
    UUID assignmentId = createAssignment(teacher);

    String created =
        mockMvc
            .perform(
                post("/api/assignments/{id}/submissions", assignmentId)
                    .with(as(studentA, "STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"A's answer\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID submissionId = UUID.fromString(JsonPath.read(created, "$.id"));

    mockMvc
        .perform(put("/api/submissions/{id}/submit", submissionId).with(as(studentA, "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/submissions/{id}/grade", submissionId)
                .with(as(teacher, "TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":91}"))
        .andExpect(status().isCreated());
  }

  /** Submissions now carry a real FK to their assignment, so tests must author one first. */
  private UUID createAssignment(UUID teacher) throws Exception {
    String created =
        mockMvc
            .perform(
                post("/api/assignments")
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"groupId\":\"%s\",\"title\":\"HW\"}".formatted(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(created, "$.id"));
  }
}
