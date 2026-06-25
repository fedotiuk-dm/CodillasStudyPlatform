package de.codillas.enrollment.mapper;

import java.util.UUID;

import de.codillas.enrollment.api.dto.AttendanceResponse;
import de.codillas.enrollment.api.dto.MarkAttendanceRequest;
import de.codillas.enrollment.domain.model.Attendance;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface AttendanceMapper {

  AttendanceResponse toResponse(Attendance entity);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "scheduledLessonId", source = "scheduledLessonId")
  Attendance toEntity(MarkAttendanceRequest request, UUID scheduledLessonId);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "present")
  void updateEntity(@MappingTarget Attendance entity, MarkAttendanceRequest request);
}
