package de.codillas.integration.course;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.codillas.integration.BaseIntegrationTest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Course structure controller (integration)")
class CourseStructureControllerIntegrationTest extends BaseIntegrationTest {

  private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
  private static final SimpleGrantedAuthority TEACHER = new SimpleGrantedAuthority("ROLE_TEACHER");
  private static final SimpleGrantedAuthority STUDENT = new SimpleGrantedAuthority("ROLE_STUDENT");

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("teacher builds section → lesson → material; student reads the nested tree")
  void builderFlow_endToEnd() throws Exception {
    String courseId = createCourse();
    String sectionId = createSection(courseId);
    String lessonId = createLesson(sectionId);

    mockMvc
        .perform(
            jsonPost("/api/lessons/" + lessonId + "/materials", TEACHER)
                .content(
                    "{\"type\":\"LINK\",\"title\":\"Slides\",\"url\":\"https://example.com/s\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("LINK"));

    // student sees the nested course tree
    mockMvc
        .perform(get("/api/courses/" + courseId).with(jwt().authorities(STUDENT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].id").value(sectionId))
        .andExpect(jsonPath("$.sections[0].lessons[0].id").value(lessonId));

    // student opens the lesson and sees its material
    mockMvc
        .perform(get("/api/lessons/" + lessonId).with(jwt().authorities(STUDENT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.materials[0].title").value("Slides"));
  }

  @Test
  @DisplayName("creating a section without the TEACHER role returns 403")
  void createSection_asStudent_returns403() throws Exception {
    String courseId = createCourse();
    mockMvc
        .perform(
            jsonPost("/api/courses/" + courseId + "/sections", STUDENT)
                .content("{\"title\":\"X\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("a FILE material with no fileId is rejected with 400")
  void addMaterial_fileWithoutFileId_returns400() throws Exception {
    String lessonId = createLesson(createSection(createCourse()));
    mockMvc
        .perform(
            jsonPost("/api/lessons/" + lessonId + "/materials", TEACHER)
                .content("{\"type\":\"FILE\",\"title\":\"Doc\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("deleting a section cascades — its lessons become 404")
  void deleteSection_cascades_lessonBecomes404() throws Exception {
    String sectionId = createSection(createCourse());
    String lessonId = createLesson(sectionId);

    mockMvc
        .perform(delete("/api/sections/" + sectionId).with(jwt().authorities(TEACHER)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/lessons/" + lessonId).with(jwt().authorities(TEACHER)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET an unknown course returns 404")
  void getCourse_unknownId_returns404() throws Exception {
    mockMvc
        .perform(
            get("/api/courses/00000000-0000-0000-0000-000000000000")
                .with(jwt().authorities(STUDENT)))
        .andExpect(status().isNotFound());
  }

  // --- helpers: build the structure and return the created id ---

  private MockHttpServletRequestBuilder jsonPost(String path, SimpleGrantedAuthority role) {
    return post(path).with(jwt().authorities(role)).contentType(MediaType.APPLICATION_JSON);
  }

  private String createdId(MockHttpServletRequestBuilder request) throws Exception {
    String body =
        mockMvc
            .perform(request)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  private String createCourse() throws Exception {
    return createdId(jsonPost("/api/courses", ADMIN).content("{\"name\":\"Java Backend\"}"));
  }

  private String createSection(String courseId) throws Exception {
    return createdId(
        jsonPost("/api/courses/" + courseId + "/sections", TEACHER)
            .content("{\"title\":\"Basics\"}"));
  }

  private String createLesson(String sectionId) throws Exception {
    return createdId(
        jsonPost("/api/sections/" + sectionId + "/lessons", TEACHER)
            .content("{\"title\":\"Intro\"}"));
  }
}
