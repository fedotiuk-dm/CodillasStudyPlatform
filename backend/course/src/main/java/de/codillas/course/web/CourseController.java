package de.codillas.course.web;

import de.codillas.course.api.CourseApi;
import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CreateCourseRequest;
import de.codillas.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Thin delegator — implements the generated {@link CourseApi}, no business logic. */
@RestController
@RequiredArgsConstructor
public class CourseController implements CourseApi {

  private final CourseService service;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CourseResponse> createCourse(CreateCourseRequest createCourseRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.createCourse(createCourseRequest));
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
  public ResponseEntity<CourseListResponse> listCourses(Pageable pageable) {
    return ResponseEntity.ok(service.listCourses(pageable));
  }
}
