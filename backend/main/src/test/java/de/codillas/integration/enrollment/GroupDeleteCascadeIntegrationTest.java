package de.codillas.integration.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.chat.domain.model.ChatRoomType;
import de.codillas.chat.domain.repository.ChatRoomRepository;
import de.codillas.enrollment.domain.model.CourseStatusView;
import de.codillas.enrollment.domain.repository.AttendanceRepository;
import de.codillas.enrollment.domain.repository.CourseStatusViewRepository;
import de.codillas.enrollment.domain.repository.GroupRepository;
import de.codillas.enrollment.domain.repository.ScheduledLessonRepository;
import de.codillas.gradebook.domain.repository.GradebookMembershipRepository;
import de.codillas.homework.domain.repository.AssignmentRepository;
import de.codillas.homework.domain.repository.SubmissionRepository;
import de.codillas.integration.BaseIntegrationTest;
import de.codillas.notification.domain.repository.NotificationMembershipRepository;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Group delete cascade (integration)")
class GroupDeleteCascadeIntegrationTest extends BaseIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired GroupRepository groups;
  @Autowired GradebookMembershipRepository gradebookMemberships;
  @Autowired NotificationMembershipRepository notificationMemberships;
  @Autowired ChatRoomRepository chatRooms;
  @Autowired AssignmentRepository assignments;
  @Autowired SubmissionRepository submissions;
  @Autowired ScheduledLessonRepository scheduledLessons;
  @Autowired AttendanceRepository attendance;
  @Autowired CourseStatusViewRepository courseStatus;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  /**
   * Create a DRAFT course, publish it, and await enrollment's local read model catching up via the
   * {@code CoursePublished} event — a group can only be created against a PUBLISHED course.
   */
  private UUID createPublishedCourse() throws Exception {
    String course =
        mockMvc
            .perform(
                post("/api/courses")
                    .with(as(UUID.randomUUID(), "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Course %s\"}".formatted(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID courseId = UUID.fromString(JsonPath.read(course, "$.id"));
    mockMvc
        .perform(post("/api/courses/{id}/publish", courseId).with(as(UUID.randomUUID(), "ADMIN")))
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

  @Test
  @DisplayName("deleting a group purges enrollment rows and every read model")
  void cascades() throws Exception {
    UUID admin = UUID.randomUUID();
    UUID teacher = UUID.randomUUID();
    UUID student = UUID.randomUUID();
    UUID courseId = createPublishedCourse();

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

    mockMvc
        .perform(
            post("/api/groups/{id}/members", groupId)
                .with(as(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"%s\"}".formatted(student)))
        .andExpect(status().isCreated());

    // Homework owned by the group: an assignment + a student submission (purged via GroupDeleted).
    String assignment =
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
    UUID assignmentId = UUID.fromString(JsonPath.read(assignment, "$.id"));

    mockMvc
        .perform(
            post("/api/assignments/{id}/submissions", assignmentId)
                .with(as(student, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"my answer\"}"))
        .andExpect(status().isCreated());

    // Schedule a lesson and mark attendance (purged by FK ON DELETE CASCADE from study_groups).
    String lesson =
        mockMvc
            .perform(
                post("/api/groups/{groupId}/lessons", groupId)
                    .with(as(admin, "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Intro\",\"scheduledAt\":\"2026-09-01T10:00:00Z\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID lessonId = UUID.fromString(JsonPath.read(lesson, "$.id"));

    mockMvc
        .perform(
            put("/api/groups/{groupId}/lessons/{lessonId}/attendance", groupId, lessonId)
                .with(as(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"%s\",\"present\":true}".formatted(student)))
        .andExpect(status().isOk());

    // Sanity: the homework + schedule rows are present before the delete.
    assertThat(assignments.findByGroupId(groupId)).isNotEmpty();
    assertThat(submissions.findByAssignmentIdIn(List.of(assignmentId))).isNotEmpty();
    assertThat(scheduledLessons.findByGroupId(groupId, ScheduledLessonRepository.BY_TIME))
        .isNotEmpty();
    assertThat(attendance.findByScheduledLessonId(lessonId)).isNotEmpty();

    // StudentEnrolled is consumed asynchronously into the read models.
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              assertThat(gradebookMemberships.findByGroupId(groupId)).isNotEmpty();
              assertThat(notificationMemberships.findByGroupId(groupId)).isNotEmpty();
              assertThat(chatRooms.findByTypeAndReferenceId(ChatRoomType.GROUP, groupId))
                  .isPresent();
            });

    mockMvc
        .perform(delete("/api/groups/{id}", groupId).with(as(admin, "ADMIN")))
        .andExpect(status().isNoContent());

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              assertThat(groups.findById(groupId)).isEmpty();
              assertThat(gradebookMemberships.findByGroupId(groupId)).isEmpty();
              assertThat(notificationMemberships.findByGroupId(groupId)).isEmpty();
              assertThat(chatRooms.findByTypeAndReferenceId(ChatRoomType.GROUP, groupId)).isEmpty();
              // Homework purged via the GroupDeleted event.
              assertThat(assignments.findByGroupId(groupId)).isEmpty();
              assertThat(submissions.findByAssignmentIdIn(List.of(assignmentId))).isEmpty();
              // scheduled_lessons + attendance gone via FK ON DELETE CASCADE from study_groups.
              assertThat(scheduledLessons.findByGroupId(groupId, ScheduledLessonRepository.BY_TIME))
                  .isEmpty();
              assertThat(attendance.findByScheduledLessonId(lessonId)).isEmpty();
            });
  }
}
