package de.codillas.integration.course;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.integration.BaseIntegrationTest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Course lifecycle endpoints (integration)")
class CourseLifecycleControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor withRole(String role) {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private String createDraftCourse(String name) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/courses")
                    .with(withRole("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"%s\"}".formatted(name)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  @Test
  @DisplayName("ADMIN publishes then archives a course (200 each, status advances)")
  void publishThenArchive_asAdmin() throws Exception {
    String id = createDraftCourse("Lifecycle A");

    mockMvc
        .perform(post("/api/courses/{id}/publish", id).with(withRole("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));

    mockMvc
        .perform(post("/api/courses/{id}/archive", id).with(withRole("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"));
  }

  @Test
  @DisplayName("publishing twice is a 409 conflict")
  void republish_conflicts() throws Exception {
    String id = createDraftCourse("Lifecycle B");
    mockMvc.perform(post("/api/courses/{id}/publish", id).with(withRole("ADMIN")));
    mockMvc
        .perform(post("/api/courses/{id}/publish", id).with(withRole("ADMIN")))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("publish without the ADMIN role returns 403")
  void publish_asStudent_returns403() throws Exception {
    String id = createDraftCourse("Lifecycle C");
    mockMvc
        .perform(post("/api/courses/{id}/publish", id).with(withRole("STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /api/courses as STUDENT never returns a DRAFT course")
  void listCourses_asStudent_excludesDraft() throws Exception {
    String draftId = createDraftCourse("Hidden Draft");
    mockMvc
        .perform(get("/api/courses").param("size", "200").with(withRole("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(draftId)).isEmpty());
  }
}
