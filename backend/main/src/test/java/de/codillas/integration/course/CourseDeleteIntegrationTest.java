package de.codillas.integration.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.model.Lesson;
import de.codillas.course.domain.model.Material;
import de.codillas.course.domain.model.MaterialType;
import de.codillas.course.domain.model.Section;
import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.course.domain.repository.LessonRepository;
import de.codillas.course.domain.repository.MaterialRepository;
import de.codillas.course.domain.repository.SectionRepository;
import de.codillas.integration.BaseIntegrationTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Course delete cascade (integration)")
class CourseDeleteIntegrationTest extends BaseIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired CourseRepository courses;
  @Autowired SectionRepository sections;
  @Autowired LessonRepository lessons;
  @Autowired MaterialRepository materials;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName("deleting a course removes its sections, lessons and materials via FK cascade")
  void deletesAndCascades() throws Exception {
    Course course = courses.save(Course.builder().name("C").build());
    Section section =
        sections.save(Section.builder().courseId(course.getId()).title("S").sortOrder(0).build());
    Lesson lesson =
        lessons.save(Lesson.builder().sectionId(section.getId()).title("L").sortOrder(0).build());
    Material material =
        materials.save(
            Material.builder()
                .lessonId(lesson.getId())
                .type(MaterialType.LINK)
                .title("M")
                .url("https://x")
                .sortOrder(0)
                .build());

    mockMvc
        .perform(delete("/api/courses/{id}", course.getId()).with(as(UUID.randomUUID(), "ADMIN")))
        .andExpect(status().isNoContent());

    assertThat(courses.findById(course.getId())).isEmpty();
    assertThat(sections.findById(section.getId())).isEmpty();
    assertThat(lessons.findById(lesson.getId())).isEmpty();
    assertThat(materials.findById(material.getId())).isEmpty();
  }
}
