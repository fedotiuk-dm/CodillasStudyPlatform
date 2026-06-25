package de.codillas.integration.course;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.integration.BaseIntegrationTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CourseController (integration)")
class CourseControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("POST /api/courses as ADMIN creates the course and returns 201 with the body")
  void createCourse_asAdmin_returns201WithBody() throws Exception {
    mockMvc
        .perform(
            post("/api/courses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Java Backend\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Java Backend"));
  }

  @Test
  @DisplayName("POST /api/courses without the ADMIN role returns 403")
  void createCourse_withoutAdminRole_returns403() throws Exception {
    mockMvc
        .perform(
            post("/api/courses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /api/courses as STUDENT returns 200 with a content array")
  void listCourses_asStudent_returns200WithContentArray() throws Exception {
    mockMvc
        .perform(
            get("/api/courses").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }
}
