package de.codillas.integration.announcement;

import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

@DisplayName("Announcements (class stream, integration)")
class AnnouncementIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName(
      "teacher posts to a group; the enrolled student reads the stream and is notified; a non-member gets 404")
  void postReadNotifyAndScope() throws Exception {
    UUID teacher = UUID.randomUUID();
    UUID member = UUID.randomUUID();
    UUID outsider = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();

    // Student create is role-blocked outright.
    mockMvc
        .perform(
            post("/api/groups/{groupId}/announcements", groupId)
                .with(as(member, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"nope\"}"))
        .andExpect(status().isForbidden());

    // A student who is not on the group's roster gets 404, not 403.
    mockMvc
        .perform(get("/api/groups/{groupId}/announcements", groupId).with(as(outsider, "STUDENT")))
        .andExpect(status().isNotFound());

    // Teacher posts (group id is a by-id reference; no group validation by design).
    String created =
        mockMvc
            .perform(
                post("/api/groups/{groupId}/announcements", groupId)
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"Exam moved\",\"body\":\"Now on Friday\",\"pinned\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Exam moved"))
            .andExpect(jsonPath("$.pinned").value(true))
            .andExpect(jsonPath("$.authorId").value(teacher.toString()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID announcementId = UUID.fromString(JsonPath.read(created, "$.id"));

    // Staff read any group's stream.
    mockMvc
        .perform(get("/api/groups/{groupId}/announcements", groupId).with(as(teacher, "TEACHER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(announcementId.toString()));

    // Feed the roster with the same event enrollment publishes (the full enroll flow needs the
    // course lifecycle and is covered by the enrollment ITs).
    publishStudentEnrolled(groupId, member);

    // The roster is event-fed (async): the member's read flips from 404 to 200.
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                mockMvc
                    .perform(
                        get("/api/groups/{groupId}/announcements", groupId)
                            .with(as(member, "STUDENT")))
                    .andExpect(status().isOk()));

    // A post after enrollment fans out to the member's notification inbox.
    mockMvc
        .perform(
            post("/api/groups/{groupId}/announcements", groupId)
                .with(as(teacher, "TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Bring laptops\"}"))
        .andExpect(status().isCreated());

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                mockMvc
                    .perform(get("/api/notifications").with(as(member, "STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(
                        jsonPath("$.content[?(@.type=='ANNOUNCEMENT_POSTED')]").isNotEmpty()));

    // The member's cross-group feed contains the group's posts, pinned first.
    mockMvc
        .perform(get("/api/me/announcements").with(as(member, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Exam moved"));

    // Edit + delete round-trip.
    mockMvc
        .perform(
            put("/api/announcements/{id}", announcementId)
                .with(as(teacher, "TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pinned\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pinned").value(false))
        .andExpect(jsonPath("$.title").value("Exam moved"));
    mockMvc
        .perform(delete("/api/announcements/{id}", announcementId).with(as(teacher, "TEACHER")))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(delete("/api/announcements/{id}", announcementId).with(as(teacher, "TEACHER")))
        .andExpect(status().isNotFound());
  }

  @Autowired private org.springframework.context.ApplicationEventPublisher events;
  @Autowired private org.springframework.transaction.support.TransactionTemplate tx;

  /** Publish StudentEnrolled the way enrollment does — inside a committed transaction. */
  private void publishStudentEnrolled(UUID groupId, UUID studentId) {
    tx.executeWithoutResult(
        _ -> events.publishEvent(new de.codillas.shared.event.StudentEnrolled(groupId, studentId)));
  }
}
