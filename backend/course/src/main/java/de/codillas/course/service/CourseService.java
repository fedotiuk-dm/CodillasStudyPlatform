package de.codillas.course.service;

import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CreateCourseRequest;
import org.springframework.data.domain.Pageable;

public interface CourseService {

  CourseResponse createCourse(CreateCourseRequest request);

  CourseListResponse listCourses(Pageable pageable);
}
