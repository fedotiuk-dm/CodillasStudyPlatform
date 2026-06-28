package de.codillas.course.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.model.CourseStatus;
import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.shared.event.CoursePublished;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseStartupReconciler — republish CoursePublished on startup")
class CourseStartupReconcilerTest {

  @Mock CourseRepository courseRepository;
  @Mock ApplicationEventPublisher events;
  @InjectMocks CourseStartupReconciler reconciler;

  @Test
  @DisplayName("publishes CoursePublished for every already-PUBLISHED course")
  void republishesForEachPublishedCourse() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    when(courseRepository.findByStatus(CourseStatus.PUBLISHED))
        .thenReturn(List.of(courseWithId(a), courseWithId(b)));

    reconciler.republishPublishedCourses();

    verify(events).publishEvent(new CoursePublished(a));
    verify(events).publishEvent(new CoursePublished(b));
  }

  @Test
  @DisplayName("publishes nothing when no course is PUBLISHED")
  void noPublishedCoursesNoEvents() {
    when(courseRepository.findByStatus(CourseStatus.PUBLISHED)).thenReturn(List.of());

    reconciler.republishPublishedCourses();

    verifyNoInteractions(events);
  }

  private static Course courseWithId(UUID id) {
    Course course = Course.builder().name("Java Backend").build();
    course.setId(id);
    return course;
  }
}
