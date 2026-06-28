package de.codillas.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CourseStatus;
import de.codillas.course.api.dto.CreateCourseRequest;
import de.codillas.course.domain.CourseStateMachine;
import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.course.mapper.CourseMapper;
import de.codillas.shared.event.CourseArchived;
import de.codillas.shared.event.CoursePublished;
import de.codillas.shared.security.CurrentUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseService")
class CourseServiceTest {

  @Mock private CourseRepository repository;
  @Mock private CourseMapper mapper;
  @Mock private CourseStateMachine stateMachine;
  @Mock private CurrentUser currentUser;
  @Mock private ApplicationEventPublisher events;
  @InjectMocks private CourseServiceImpl service;

  @Test
  @DisplayName("createCourse maps the request, persists it, and returns the response DTO")
  void createCourse_mapsPersistsAndReturnsResponse() {
    CreateCourseRequest request = new CreateCourseRequest("Java Backend");
    Course toSave = Course.builder().name("Java Backend").build();
    Course saved = Course.builder().name("Java Backend").build();
    CourseResponse response =
        new CourseResponse(UUID.randomUUID(), "Java Backend", CourseStatus.DRAFT);

    when(mapper.toEntity(request)).thenReturn(toSave);
    when(repository.save(toSave)).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(response);

    assertThat(service.createCourse(request)).isSameAs(response);
    verify(repository).save(toSave);
  }

  @Test
  @DisplayName("listCourses for staff with no filter maps the repository page to the list response")
  void listCourses_returnsMappedRepositoryPage() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Course> page = new PageImpl<>(List.of(Course.builder().name("Java Backend").build()));
    CourseListResponse expected = mock(CourseListResponse.class);

    when(currentUser.isStaff()).thenReturn(true);
    when(repository.findAll(pageable)).thenReturn(page);
    when(mapper.toListResponse(page)).thenReturn(expected);

    assertThat(service.listCourses(null, pageable)).isSameAs(expected);
  }

  @Test
  @DisplayName("publishCourse transitions, saves, and publishes a CoursePublished event")
  void publishCourse_publishesEvent() {
    UUID id = UUID.randomUUID();
    Course course = Course.builder().name("Java Backend").build();
    CourseResponse response = new CourseResponse(id, "Java Backend", CourseStatus.PUBLISHED);

    when(repository.findById(id)).thenReturn(Optional.of(course));
    when(repository.save(course)).thenReturn(course);
    when(mapper.toResponse(course)).thenReturn(response);

    assertThat(service.publishCourse(id)).isSameAs(response);
    verify(stateMachine)
        .transitionTo(course, de.codillas.course.domain.model.CourseStatus.PUBLISHED);
    verify(events).publishEvent(new CoursePublished(id));
  }

  @Test
  @DisplayName("archiveCourse transitions, saves, and publishes a CourseArchived event")
  void archiveCourse_publishesEvent() {
    UUID id = UUID.randomUUID();
    Course course = Course.builder().name("Java Backend").build();
    CourseResponse response = new CourseResponse(id, "Java Backend", CourseStatus.ARCHIVED);

    when(repository.findById(id)).thenReturn(Optional.of(course));
    when(repository.save(course)).thenReturn(course);
    when(mapper.toResponse(course)).thenReturn(response);

    assertThat(service.archiveCourse(id)).isSameAs(response);
    verify(stateMachine)
        .transitionTo(course, de.codillas.course.domain.model.CourseStatus.ARCHIVED);
    verify(events).publishEvent(new CourseArchived(id));
  }
}
