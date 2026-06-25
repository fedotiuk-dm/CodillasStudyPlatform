package de.codillas.integration.notification;

import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
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

@DisplayName("Notification (event-fed, integration)")
class NotificationIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName("grading a submission notifies the student, who can then mark it read")
  void gradedSubmission_notifiesAndMarksRead() throws Exception {
    UUID teacher = UUID.randomUUID();
    UUID student = UUID.randomUUID();

    String assignment =
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
    UUID assignmentId = UUID.fromString(JsonPath.read(assignment, "$.id"));

    String submission =
        mockMvc
            .perform(
                post("/api/assignments/{id}/submissions", assignmentId)
                    .with(as(student, "STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"answer\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID submissionId = UUID.fromString(JsonPath.read(submission, "$.id"));

    mockMvc
        .perform(put("/api/submissions/{id}/submit", submissionId).with(as(student, "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/submissions/{id}/grade", submissionId)
                .with(as(teacher, "TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":77}"))
        .andExpect(status().isCreated());

    // SubmissionGraded is delivered asynchronously via the publication registry.
    String[] notificationId = new String[1];
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              String body =
                  mockMvc
                      .perform(get("/api/notifications").with(as(student, "STUDENT")))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.unread").value(1))
                      .andExpect(jsonPath("$.content[0].type").value("SUBMISSION_GRADED"))
                      .andExpect(
                          jsonPath("$.content[0].referenceId").value(submissionId.toString()))
                      .andReturn()
                      .getResponse()
                      .getContentAsString();
              notificationId[0] = JsonPath.read(body, "$.content[0].id");
            });

    mockMvc
        .perform(
            post("/api/notifications/{id}/read", UUID.fromString(notificationId[0]))
                .with(as(student, "STUDENT")))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/notifications").with(as(student, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unread").value(0));
  }
}
