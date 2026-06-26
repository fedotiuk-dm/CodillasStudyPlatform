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

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Attendance read endpoint (integration)")
class AttendanceControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor admin() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  private UUID createGroup() throws Exception {
    String group =
        mockMvc
            .perform(
                post("/api/groups")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Cohort A\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
                            .formatted(UUID.randomUUID(), UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(group, "$.id"));
  }

  private UUID scheduleLesson(UUID groupId, String title) throws Exception {
    String lesson =
        mockMvc
            .perform(
                post("/api/groups/{groupId}/lessons", groupId)
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"%s\",\"scheduledAt\":\"2026-09-01T10:00:00Z\"}"
                            .formatted(title)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(lesson, "$.id"));
  }

  private void mark(UUID groupId, UUID lessonId, UUID userId, boolean present) throws Exception {
    mockMvc
        .perform(
            put("/api/groups/{groupId}/lessons/{scheduledLessonId}/attendance", groupId, lessonId)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"%s\",\"present\":%s}".formatted(userId, present)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName(
      "GET attendance returns every marked student with the correct present flag, and re-marking does not duplicate")
  void listAttendance_returnsMarkedStudents_withoutDuplicates() throws Exception {
    UUID groupId = createGroup();
    UUID lessonId = scheduleLesson(groupId, "Intro");
    UUID presentUser = UUID.randomUUID();
    UUID absentUser = UUID.randomUUID();

    mark(groupId, lessonId, presentUser, true);
    mark(groupId, lessonId, absentUser, false);

    mockMvc
        .perform(
            get("/api/groups/{groupId}/lessons/{scheduledLessonId}/attendance", groupId, lessonId)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(
            jsonPath("$[?(@.userId=='%s')].present".formatted(presentUser))
                .value(Matchers.contains(true)))
        .andExpect(
            jsonPath("$[?(@.userId=='%s')].present".formatted(absentUser))
                .value(Matchers.contains(false)));

    // Re-marking the same student upserts in place — no duplicate row.
    mark(groupId, lessonId, presentUser, false);

    mockMvc
        .perform(
            get("/api/groups/{groupId}/lessons/{scheduledLessonId}/attendance", groupId, lessonId)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(
            jsonPath("$[?(@.userId=='%s')].present".formatted(presentUser))
                .value(Matchers.contains(false)));
  }
}
