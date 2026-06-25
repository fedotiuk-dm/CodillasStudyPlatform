package de.codillas.course.web;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import de.codillas.course.api.CourseApi;
import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CreateCourseRequest;
import de.codillas.course.service.CourseService;
import de.codillas.shared.security.RequiresAdmin;
import de.codillas.shared.security.RequiresAuthenticated;

import lombok.RequiredArgsConstructor;

/** Thin delegator — implements the generated {@link CourseApi}, no business logic. */
@RestController
@RequiredArgsConstructor
public class CourseController implements CourseApi {

  private final CourseService service;

  @Override
  @RequiresAdmin
  public ResponseEntity<CourseResponse> createCourse(CreateCourseRequest createCourseRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.createCourse(createCourseRequest));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<CourseListResponse> listCourses(Pageable pageable) {
    return ResponseEntity.ok(service.listCourses(pageable));
  }
}
