package de.codillas.integration.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
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

import de.codillas.enrollment.domain.model.CourseStatusView;
import de.codillas.enrollment.domain.repository.CourseStatusViewRepository;
import de.codillas.integration.BaseIntegrationTest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EnrollmentController (integration)")
class EnrollmentControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CourseStatusViewRepository courseStatus;

  private static JwtRequestPostProcessor withRole(String role) {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  /**
   * Create a DRAFT course, publish it, and await enrollment's local read model catching up via the
   * {@code CoursePublished} event — a group can only be created against a PUBLISHED course, and
   * enrollment learns that by event, never by importing course internals.
   */
  private UUID createPublishedCourse() throws Exception {
    String course =
        mockMvc
            .perform(
                post("/api/courses")
                    .with(withRole("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Course %s\"}".formatted(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID courseId = UUID.fromString(JsonPath.read(course, "$.id"));
    mockMvc
        .perform(post("/api/courses/{id}/publish", courseId).with(withRole("ADMIN")))
        .andExpect(status().isOk());
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                assertThat(courseStatus.findById(courseId))
                    .get()
                    .extracting(CourseStatusView::getStatus)
                    .isEqualTo("PUBLISHED"));
    return courseId;
  }

  /**
   * Persist a real study_group (against a published course) so group-scoped FKs are satisfiable.
   */
  private UUID createGroup() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/groups")
                    .with(withRole("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Cohort\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
                            .formatted(createPublishedCourse(), UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(body, "$.id"));
  }

  /** Persist a real scheduled_lesson under {@code groupId} so attendance's FK is satisfiable. */
  private UUID scheduleLesson(UUID groupId) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/groups/{groupId}/lessons", groupId)
                    .with(withRole("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Intro\",\"scheduledAt\":\"2026-09-01T10:00:00Z\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(body, "$.id"));
  }

  @Test
  @DisplayName("POST /api/groups against a PUBLISHED course as ADMIN creates a DRAFT group (201)")
  void createGroup_asAdmin_returns201() throws Exception {
    String body =
        "{\"name\":\"Cohort A\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
            .formatted(createPublishedCourse(), UUID.randomUUID());
    mockMvc
        .perform(
            post("/api/groups")
                .with(withRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Cohort A"))
        .andExpect(jsonPath("$.status").value("DRAFT"));
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
    UUID groupId = createGroup();
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
    UUID groupId = createGroup();

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
    UUID groupId = createGroup();
    UUID lessonId = scheduleLesson(groupId);
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
