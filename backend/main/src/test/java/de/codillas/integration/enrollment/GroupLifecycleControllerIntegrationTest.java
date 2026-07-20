package de.codillas.integration.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/**
 * Group lifecycle, end-to-end and fully decoupled from the course module: enrollment learns a
 * course is PUBLISHED only through the {@code CoursePublished} event (Awaitility-awaited on
 * enrollment's local read model), never by importing course internals.
 */
@DisplayName("Group lifecycle endpoints (integration)")
class GroupLifecycleControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CourseStatusViewRepository courseStatus;

  private static JwtRequestPostProcessor withRole(String role) {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  /**
   * Create a DRAFT course, publish it, and await enrollment's read model catching up via events.
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
    awaitPublished(courseId);
    return courseId;
  }

  private void awaitPublished(UUID courseId) {
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                assertThat(courseStatus.findById(courseId))
                    .get()
                    .extracting(CourseStatusView::getStatus)
                    .isEqualTo("PUBLISHED"));
  }

  private UUID createGroupFor(UUID courseId) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/groups")
                    .with(withRole("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Cohort\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
                            .formatted(courseId, UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(body, "$.id"));
  }

  @Test
  @DisplayName("create a group against a PUBLISHED course (events-fed read model) succeeds (201)")
  void createGroup_publishedCourse_succeeds() throws Exception {
    createGroupFor(createPublishedCourse());
  }

  @Test
  @DisplayName("creating a group against a DRAFT (unpublished) course is a 409")
  void createGroup_draftCourse_conflicts() throws Exception {
    String draft =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/api/courses")
                        .with(withRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Draft Course\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");
    mockMvc
        .perform(
            post("/api/groups")
                .with(withRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"X\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
                        .formatted(draft, UUID.randomUUID())))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("ADMIN starts then archives a group (status advances)")
  void startThenArchive_asAdmin() throws Exception {
    UUID group = createGroupFor(createPublishedCourse());
    mockMvc
        .perform(post("/api/groups/{id}/start", group).with(withRole("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RUNNING"));
    mockMvc
        .perform(post("/api/groups/{id}/archive", group).with(withRole("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"));
  }

  @Test
  @DisplayName("starting an ARCHIVED group resumes it — the same endpoint un-archives")
  void restartArchived_resumes() throws Exception {
    UUID group = createGroupFor(createPublishedCourse());
    mockMvc
        .perform(post("/api/groups/{id}/archive", group).with(withRole("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"));
    mockMvc
        .perform(post("/api/groups/{id}/start", group).with(withRole("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RUNNING"));
  }

  @Test
  @DisplayName("start without the ADMIN role returns 403")
  void start_asStudent_returns403() throws Exception {
    UUID group = createGroupFor(createPublishedCourse());
    mockMvc
        .perform(post("/api/groups/{id}/start", group).with(withRole("STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("enrolling into an ARCHIVED group is a 409")
  void enroll_archivedGroup_conflicts() throws Exception {
    UUID group = createGroupFor(createPublishedCourse());
    mockMvc
        .perform(post("/api/groups/{id}/archive", group).with(withRole("ADMIN")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/groups/{id}/members", group)
                .with(withRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"%s\"}".formatted(UUID.randomUUID())))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("scheduling a lesson in an ARCHIVED group is a 409")
  void schedule_archivedGroup_conflicts() throws Exception {
    UUID group = createGroupFor(createPublishedCourse());
    mockMvc
        .perform(post("/api/groups/{id}/archive", group).with(withRole("ADMIN")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/groups/{id}/lessons", group)
                .with(withRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"L\",\"scheduledAt\":\"2026-09-01T10:00:00Z\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("/api/me/groups excludes DRAFT groups and includes them once RUNNING")
  void myGroups_excludesDraftUntilStarted() throws Exception {
    UUID group = createGroupFor(createPublishedCourse());
    UUID student = UUID.randomUUID();
    mockMvc
        .perform(
            post("/api/groups/{id}/members", group)
                .with(withRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"%s\"}".formatted(student)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/me/groups").with(as(student, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0)); // still DRAFT

    mockMvc
        .perform(post("/api/groups/{id}/start", group).with(withRole("ADMIN")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/me/groups").with(as(student, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(group.toString()));
  }
}
