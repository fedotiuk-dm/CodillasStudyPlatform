package de.codillas.course.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.model.Lesson;
import de.codillas.course.domain.model.Section;
import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.course.domain.repository.LessonRepository;
import de.codillas.course.domain.repository.MaterialRepository;
import de.codillas.course.domain.repository.SectionRepository;
import de.codillas.course.mapper.CourseMapper;
import de.codillas.course.mapper.LessonMapper;
import de.codillas.shared.event.CourseDeleted;
import de.codillas.shared.event.LessonsDeleted;
import de.codillas.shared.exception.NotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
  @DisplayName("announces the deleted lessons before the cascade wipes them")
  void publishesLessonsDeleted() {
    UUID id = UUID.randomUUID();
    UUID sectionId = UUID.randomUUID();
    UUID lessonId = UUID.randomUUID();
    when(courseRepository.findById(id)).thenReturn(Optional.of(Course.builder().build()));
    when(sectionRepository.findByCourseId(id, SectionRepository.BY_ORDER))
        .thenReturn(List.of(Section.builder().id(sectionId).build()));
    when(lessonRepository.findBySectionIdIn(List.of(sectionId), LessonRepository.BY_ORDER))
        .thenReturn(List.of(Lesson.builder().id(lessonId).build()));

    service.deleteCourse(id);

    // The ids must be read before delete() — ON DELETE CASCADE removes the lessons inside the
    // database, where no listener can see them.
    InOrder order = inOrder(lessonRepository, courseRepository, events);
    order.verify(lessonRepository).findBySectionIdIn(List.of(sectionId), LessonRepository.BY_ORDER);
    order.verify(courseRepository).delete(org.mockito.ArgumentMatchers.any());
    order.verify(events).publishEvent(new LessonsDeleted(List.of(lessonId)));
  }

  @Test
  @DisplayName("404 when the course does not exist")
  void missing404() {
    UUID id = UUID.randomUUID();
    when(courseRepository.findById(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.deleteCourse(id)).isInstanceOf(NotFoundException.class);
  }
}
