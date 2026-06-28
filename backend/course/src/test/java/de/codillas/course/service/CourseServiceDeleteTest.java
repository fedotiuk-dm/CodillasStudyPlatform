package de.codillas.course.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.course.domain.repository.LessonRepository;
import de.codillas.course.domain.repository.MaterialRepository;
import de.codillas.course.domain.repository.SectionRepository;
import de.codillas.course.mapper.CourseMapper;
import de.codillas.course.mapper.LessonMapper;
import de.codillas.shared.event.CourseDeleted;
import de.codillas.shared.exception.NotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseServiceImpl — delete course")
class CourseServiceDeleteTest {
  @Mock CourseRepository courseRepository;
  @Mock SectionRepository sectionRepository;
  @Mock LessonRepository lessonRepository;
  @Mock MaterialRepository materialRepository;
  @Mock CourseMapper courseMapper;
  @Mock LessonMapper lessonMapper;
  @Mock ApplicationEventPublisher events;
  @InjectMocks CourseServiceImpl service;

  @Test
  @DisplayName("deletes the course and publishes CourseDeleted")
  void deletesAndPublishes() {
    UUID id = UUID.randomUUID();
    Course course = Course.builder().build();
    when(courseRepository.findById(id)).thenReturn(Optional.of(course));

    service.deleteCourse(id);

    verify(courseRepository).delete(course);
    verify(events).publishEvent(new CourseDeleted(id));
  }

  @Test
  @DisplayName("404 when the course does not exist")
  void missing404() {
    UUID id = UUID.randomUUID();
    when(courseRepository.findById(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.deleteCourse(id)).isInstanceOf(NotFoundException.class);
  }
}
