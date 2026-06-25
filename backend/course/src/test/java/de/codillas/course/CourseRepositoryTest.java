package de.codillas.course;

import static org.assertj.core.api.Assertions.assertThat;

import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
    classes = CourseTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class CourseRepositoryTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add(
        "spring.liquibase.change-log", () -> "classpath:db/changelog/course-changelog.yaml");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
  }

  @Autowired private CourseRepository repository;

  @Test
  void savedCourse_getsCreatedAtTimestamp() {
    Course saved = repository.save(Course.builder().name("Java Backend").build());

    Course found = repository.findById(saved.getId()).orElseThrow();

    assertThat(found.getName()).isEqualTo("Java Backend");
    assertThat(found.getCreatedAt()).isNotNull();
  }
}
