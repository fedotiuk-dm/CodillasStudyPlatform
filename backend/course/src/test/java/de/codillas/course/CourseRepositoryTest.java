package de.codillas.course;

import static org.assertj.core.api.Assertions.assertThat;

import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = CourseTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CourseRepositoryTest extends PostgresTestContainer {

  @Autowired private CourseRepository repository;

  @Test
  void savedCourse_getsCreatedAtTimestamp() {
    Course saved = repository.save(Course.builder().name("Java Backend").build());

    Course found = repository.findById(saved.getId()).orElseThrow();

    assertThat(found.getName()).isEqualTo("Java Backend");
    assertThat(found.getCreatedAt()).isNotNull();
  }
}
