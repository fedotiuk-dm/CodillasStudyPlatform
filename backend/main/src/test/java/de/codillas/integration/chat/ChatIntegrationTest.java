package de.codillas.integration.chat;

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

import de.codillas.integration.BaseIntegrationTest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Chat (integration)")
class ChatIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName("create a direct room, post a message, read history; a non-member is refused")
  void directRoomMessaging() throws Exception {
    UUID teacher = UUID.randomUUID();
    UUID student = UUID.randomUUID();

    String room =
        mockMvc
            .perform(
                post("/api/chat/rooms")
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"DIRECT\",\"memberIds\":[\"%s\"]}".formatted(student)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("DIRECT"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID roomId = UUID.fromString(JsonPath.read(room, "$.id"));

    mockMvc
        .perform(
            post("/api/chat/rooms/{roomId}/messages", roomId)
                .with(as(student, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"hello teacher\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("hello teacher"))
        .andExpect(jsonPath("$.senderId").value(student.toString()));

    mockMvc
        .perform(get("/api/chat/rooms/{roomId}/messages", roomId).with(as(teacher, "TEACHER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].content").value("hello teacher"));

    mockMvc
        .perform(
            get("/api/chat/rooms/{roomId}/messages", roomId).with(as(UUID.randomUUID(), "STUDENT")))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("enrolling a student creates the group channel and adds them as a member")
  void enrolment_createsGroupChannel() throws Exception {
    UUID admin = UUID.randomUUID();
    UUID student = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/groups/{groupId}/members", groupId)
                .with(as(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"%s\"}".formatted(student)))
        .andExpect(status().isCreated());

    // StudentEnrolled is delivered asynchronously via the publication registry.
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                mockMvc
                    .perform(get("/api/chat/rooms").with(as(student, "STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].type").value("GROUP"))
                    .andExpect(jsonPath("$[0].referenceId").value(groupId.toString())));
  }
}
