package de.codillas.course.service;

import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CreateCourseRequest;
import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.course.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

  private final CourseRepository repository;
  private final CourseMapper mapper;

  @Override
  @Transactional
  public CourseResponse createCourse(CreateCourseRequest request) {
    return mapper.toResponse(repository.save(mapper.toEntity(request)));
  }

  @Override
  public CourseListResponse listCourses(Pageable pageable) {
    return mapper.toListResponse(repository.findAll(pageable));
  }
}
