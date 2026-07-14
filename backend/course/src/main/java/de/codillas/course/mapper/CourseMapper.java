package de.codillas.course.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import de.codillas.course.api.dto.CourseDetailResponse;
import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CreateCourseRequest;
import de.codillas.course.api.dto.CreateSectionRequest;
import de.codillas.course.api.dto.LessonSummary;
import de.codillas.course.api.dto.SectionResponse;
import de.codillas.course.api.dto.UpdateSectionRequest;
import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.model.Lesson;
import de.codillas.course.domain.model.Section;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.*;

@Mapper(config = CentralMapperConfig.class)
public interface CourseMapper {

  CourseResponse toResponse(Course entity);

  de.codillas.course.domain.model.CourseStatus toDomainStatus(
      de.codillas.course.api.dto.CourseStatus status);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Course toEntity(CreateCourseRequest request);

  CourseListResponse toListResponse(Page<Course> page);

  CourseDetailResponse toDetailResponse(Course course, List<SectionResponse> sections);

  SectionResponse toSectionResponse(Section section, List<LessonSummary> lessons);

  LessonSummary toLessonSummary(Lesson lesson);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Section toSectionEntity(CreateSectionRequest request, UUID courseId, int sortOrder);

  @BeanMapping(
      ignoreByDefault = true,
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "title")
  void updateSection(@MappingTarget Section section, UpdateSectionRequest request);
}
