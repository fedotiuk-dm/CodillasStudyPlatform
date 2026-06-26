package de.codillas.integration.enrollment;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@DisplayName("Current-user enrollment endpoints (integration)")
class MeEnrollmentControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor admin() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
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

  private void enroll(UUID groupId, UUID userId) throws Exception {
    mockMvc
        .perform(
            post("/api/groups/{groupId}/members", groupId)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"%s\"}".formatted(userId)))
        .andExpect(status().isCreated());
  }

  private void scheduleLesson(UUID groupId, String title) throws Exception {
    mockMvc
        .perform(
            post("/api/groups/{groupId}/lessons", groupId)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"%s\",\"scheduledAt\":\"2026-09-01T10:00:00Z\"}".formatted(title)))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("a member of one group sees only its own groups and schedule, never another group's")
  void crossUserIsolation_eachSeesOnlyOwn() throws Exception {
    UUID groupA = createGroup();
    UUID studentA = UUID.randomUUID();
    enroll(groupA, studentA);
    scheduleLesson(groupA, "Lesson A");

    UUID groupB = createGroup();
    UUID studentB = UUID.randomUUID();
    enroll(groupB, studentB);
    scheduleLesson(groupB, "Lesson B");

    mockMvc
        .perform(get("/api/me/groups").with(as(studentA, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(groupA.toString()));
    mockMvc
        .perform(get("/api/me/schedule").with(as(studentA, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("Lesson A"));

    mockMvc
        .perform(get("/api/me/groups").with(as(studentB, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(groupB.toString()));
    mockMvc
        .perform(get("/api/me/schedule").with(as(studentB, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("Lesson B"));
  }

  @Test
  @DisplayName("GET /api/me/groups returns only the groups the authenticated user belongs to")
  void listMyGroups_returnsEnrolledGroups() throws Exception {
    UUID groupId = createGroup();
    UUID student = UUID.randomUUID();
    enroll(groupId, student);

    mockMvc
        .perform(get("/api/me/groups").with(as(student, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(groupId.toString()));

    mockMvc
        .perform(get("/api/me/groups").with(as(UUID.randomUUID(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("GET /api/me/schedule returns the lessons scheduled in the user's groups")
  void listMySchedule_returnsScheduledLessons() throws Exception {
    UUID groupId = createGroup();
    UUID student = UUID.randomUUID();
    enroll(groupId, student);

    mockMvc
        .perform(
            post("/api/groups/{groupId}/lessons", groupId)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Intro\",\"scheduledAt\":\"2026-09-01T10:00:00Z\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/me/schedule").with(as(student, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].groupId").value(groupId.toString()))
        .andExpect(jsonPath("$[0].title").value("Intro"));

    mockMvc
        .perform(get("/api/me/schedule").with(as(UUID.randomUUID(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
