package de.codillas.integration.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/**
 * Proves the {@code ADMIN > TEACHER > STUDENT} role hierarchy: a higher role satisfies a
 * lower-role-guarded endpoint without holding that role explicitly, while a lower role is still
 * denied. Uses {@code createSection} (guarded by {@code @RequiresTeacher}) as the probe.
 */
@DisplayName("Role hierarchy (integration)")
class RoleHierarchyIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static JwtRequestPostProcessor as(String role) {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private String createCourseAsAdmin() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/courses")
                    .with(as("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Java\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  @Test
  @DisplayName("ADMIN satisfies a @RequiresTeacher endpoint (hierarchy: ADMIN implies TEACHER)")
  void admin_passesTeacherEndpoint() throws Exception {
    String courseId = createCourseAsAdmin();
    mockMvc
        .perform(
            post("/api/courses/{courseId}/sections", courseId)
                .with(as("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Basics\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("STUDENT is still denied a @RequiresTeacher endpoint (no upward implication)")
  void student_deniedTeacherEndpoint() throws Exception {
    String courseId = createCourseAsAdmin();
    mockMvc
        .perform(
            post("/api/courses/{courseId}/sections", courseId)
                .with(as("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Basics\"}"))
        .andExpect(status().isForbidden());
  }
}
