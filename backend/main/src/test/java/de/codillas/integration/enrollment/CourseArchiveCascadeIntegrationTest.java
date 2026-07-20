package de.codillas.integration.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.enrollment.domain.model.CourseStatusView;
import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.model.GroupStatus;
import de.codillas.enrollment.domain.repository.CourseStatusViewRepository;
import de.codillas.enrollment.domain.repository.GroupRepository;
import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.repository.AssignmentRepository;
import de.codillas.integration.BaseIntegrationTest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Course archive cascade (integration)")
class CourseArchiveCascadeIntegrationTest extends BaseIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired GroupRepository groups;
  @Autowired AssignmentRepository assignments;
  @Autowired CourseStatusViewRepository courseStatus;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  /**
   * Archiving a course has to reach homework two hops away: CourseArchived → enrollment retires the
   * cohorts → GroupArchived → homework mutes their deadline reminders. Per-module unit tests cover
   * each hop; only this proves the chain is actually wired end to end.
   */
  @Test
  @DisplayName("archiving a course retires its cohorts and mutes their deadline reminders")
  void cascadesToReminders() throws Exception {
    UUID admin = UUID.randomUUID();
    UUID teacher = UUID.randomUUID();
    UUID courseId = createPublishedCourse(admin);

    String group =
        mockMvc
            .perform(
                post("/api/groups")
                    .with(as(admin, "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"G\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
                            .formatted(courseId, teacher)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID groupId = UUID.fromString(JsonPath.read(group, "$.id"));

    String assignment =
        mockMvc
            .perform(
                post("/api/assignments")
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"groupId\":\"%s\",\"title\":\"HW1\",\"dueAt\":\"2026-12-01T10:00:00Z\"}"
                            .formatted(groupId)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID assignmentId = UUID.fromString(JsonPath.read(assignment, "$.id"));
    assertThat(assignments.findById(assignmentId))
        .get()
        .extracting(Assignment::isDueReminderSent)
        .isEqualTo(false);

    mockMvc
        .perform(post("/api/courses/{id}/archive", courseId).with(as(admin, "ADMIN")))
        .andExpect(status().isOk());

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              assertThat(groups.findById(groupId))
                  .get()
                  .extracting(Group::getStatus)
                  .isEqualTo(GroupStatus.ARCHIVED);
              assertThat(assignments.findById(assignmentId))
                  .get()
                  .extracting(Assignment::isDueReminderSent)
                  .isEqualTo(true);
            });
  }

  private UUID createPublishedCourse(UUID admin) throws Exception {
    String course =
        mockMvc
            .perform(
                post("/api/courses")
                    .with(as(admin, "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Course %s\"}".formatted(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID courseId = UUID.fromString(JsonPath.read(course, "$.id"));
    mockMvc
        .perform(post("/api/courses/{id}/publish", courseId).with(as(admin, "ADMIN")))
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
}
