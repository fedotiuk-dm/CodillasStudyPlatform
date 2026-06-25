package de.codillas.integration.enrollment;

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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EnrollmentController (integration)")
class EnrollmentControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor withRole(String role) {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName("POST /api/groups as ADMIN creates the group (201)")
  void createGroup_asAdmin_returns201() throws Exception {
    String body =
        "{\"name\":\"Cohort A\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
            .formatted(UUID.randomUUID(), UUID.randomUUID());
    mockMvc
        .perform(
            post("/api/groups")
                .with(withRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Cohort A"));
  }

  @Test
  @DisplayName("POST /api/groups without the ADMIN role returns 403")
  void createGroup_withoutAdmin_returns403() throws Exception {
    mockMvc
        .perform(
            post("/api/groups")
                .with(withRole("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"X\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
                        .formatted(UUID.randomUUID(), UUID.randomUUID())))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("enrol a student (201) then list the group's members (200)")
  void enrollStudent_thenListMembers() throws Exception {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/groups/{groupId}/members", groupId)
                .with(withRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"%s\"}".formatted(userId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.groupId").value(groupId.toString()))
        .andExpect(jsonPath("$.userId").value(userId.toString()));

    mockMvc
        .perform(get("/api/groups/{groupId}/members", groupId).with(withRole("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].userId").value(userId.toString()));
  }

  @Test
  @DisplayName("schedule a lesson (201) then list the group's lessons (200)")
  void scheduleLesson_thenList() throws Exception {
    UUID groupId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/groups/{groupId}/lessons", groupId)
                .with(withRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Intro\",\"scheduledAt\":\"2026-09-01T10:00:00Z\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Intro"));

    mockMvc
        .perform(get("/api/groups/{groupId}/lessons", groupId).with(withRole("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Intro"));
  }

  @Test
  @DisplayName("markAttendance upserts the student's attendance (200)")
  void markAttendance_upserts() throws Exception {
    UUID groupId = UUID.randomUUID();
    UUID lessonId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc
        .perform(
            put("/api/groups/{groupId}/lessons/{scheduledLessonId}/attendance", groupId, lessonId)
                .with(withRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"%s\",\"present\":true}".formatted(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.present").value(true));
  }
}
