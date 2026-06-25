package de.codillas.enrollment.mapper;

import java.util.List;
import java.util.UUID;

import de.codillas.enrollment.api.dto.ScheduleLessonRequest;
import de.codillas.enrollment.api.dto.ScheduledLessonResponse;
import de.codillas.enrollment.domain.model.ScheduledLesson;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface ScheduledLessonMapper {

  ScheduledLessonResponse toResponse(ScheduledLesson entity);

  List<ScheduledLessonResponse> toResponseList(List<ScheduledLesson> entities);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "groupId", source = "groupId")
  ScheduledLesson toEntity(ScheduleLessonRequest request, UUID groupId);
}
