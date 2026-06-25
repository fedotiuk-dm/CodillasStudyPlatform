package de.codillas.integration.course;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;

import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.integration.BaseIntegrationTest;

import org.junit.jupiter.api.Test;

class CourseRepositoryTest extends BaseIntegrationTest {

  @Autowired private CourseRepository repository;

  @Test
  void savedCourse_getsCreatedAtTimestamp() {
    Course saved = repository.save(Course.builder().name("Java Backend").build());

    Course found = repository.findById(saved.getId()).orElseThrow();

    assertThat(found.getName()).isEqualTo("Java Backend");
    assertThat(found.getCreatedAt()).isNotNull();
  }
}
