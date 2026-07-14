package de.codillas.course.mapper;

import java.util.List;
import java.util.UUID;

import de.codillas.course.api.dto.CreateLessonRequest;
import de.codillas.course.api.dto.CreateMaterialRequest;
import de.codillas.course.api.dto.LessonResponse;
import de.codillas.course.api.dto.MaterialResponse;
import de.codillas.course.api.dto.UpdateLessonRequest;
import de.codillas.course.domain.model.Lesson;
import de.codillas.course.domain.model.Material;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface LessonMapper {

  LessonResponse toResponse(Lesson lesson, List<MaterialResponse> materials);

  MaterialResponse toMaterialResponse(Material material);

  List<MaterialResponse> toMaterialResponses(List<Material> materials);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Lesson toEntity(CreateLessonRequest request, UUID sectionId, int sortOrder);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Material toMaterialEntity(CreateMaterialRequest request, UUID lessonId, int sortOrder);

  @BeanMapping(
      ignoreByDefault = true,
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "title")
  @Mapping(target = "summary")
  @Mapping(target = "meetingUrl")
  @Mapping(target = "recordingUrl")
  void updateLesson(@MappingTarget Lesson lesson, UpdateLessonRequest request);
}
