package de.codillas.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CreateCourseRequest;
import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.course.mapper.CourseMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

  @Mock private CourseRepository repository;
  @Mock private CourseMapper mapper;
  @InjectMocks private CourseServiceImpl service;

  @Test
  void createCourse_mapsPersistsAndReturnsResponse() {
    CreateCourseRequest request = new CreateCourseRequest("Java Backend");
    Course toSave = Course.builder().name("Java Backend").build();
    Course saved = Course.builder().name("Java Backend").build();
    CourseResponse response = new CourseResponse(UUID.randomUUID(), "Java Backend");

    when(mapper.toEntity(request)).thenReturn(toSave);
    when(repository.save(toSave)).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(response);

    CourseResponse result = service.createCourse(request);

    assertThat(result).isSameAs(response);
    verify(repository).save(toSave);
  }

  @Test
  void listCourses_returnsMappedRepositoryPage() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Course> page = new PageImpl<>(List.of(Course.builder().name("Java Backend").build()));
    CourseListResponse expected = mock(CourseListResponse.class);

    when(repository.findAll(pageable)).thenReturn(page);
    when(mapper.toListResponse(page)).thenReturn(expected);

    CourseListResponse result = service.listCourses(pageable);

    assertThat(result).isSameAs(expected);
  }
}
