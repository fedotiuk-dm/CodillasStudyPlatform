package de.codillas.course.mapper;

import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CreateCourseRequest;
import de.codillas.course.domain.model.Course;
import de.codillas.shared.mapper.CentralMapperConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(config = CentralMapperConfig.class)
public interface CourseMapper {

  CourseResponse toResponse(Course entity);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Course toEntity(CreateCourseRequest request);

  CourseListResponse toListResponse(Page<Course> page);
}
