package de.codillas.course.service;

import org.springframework.data.domain.Pageable;

import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CreateCourseRequest;

public interface CourseService {

  CourseResponse createCourse(CreateCourseRequest request);

  CourseListResponse listCourses(Pageable pageable);
}
