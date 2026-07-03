package de.codillas.announcement.mapper;

import java.util.UUID;

import org.springframework.data.domain.Page;

import de.codillas.announcement.api.dto.AnnouncementListResponse;
import de.codillas.announcement.api.dto.AnnouncementResponse;
import de.codillas.announcement.api.dto.CreateAnnouncementRequest;
import de.codillas.announcement.api.dto.UpdateAnnouncementRequest;
import de.codillas.announcement.domain.model.Announcement;
import de.codillas.announcement.domain.model.AnnouncementMembership;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface AnnouncementMapper {

  AnnouncementResponse toResponse(Announcement entity);

  AnnouncementListResponse toListResponse(Page<Announcement> page);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "groupId", source = "groupId")
  @Mapping(target = "authorId", source = "authorId")
  Announcement toEntity(UUID groupId, UUID authorId, CreateAnnouncementRequest request);

  @BeanMapping(
      ignoreByDefault = true,
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "title")
  @Mapping(target = "body")
  @Mapping(target = "pinned")
  void updateEntity(@MappingTarget Announcement entity, UpdateAnnouncementRequest request);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "studentId", source = "userId")
  AnnouncementMembership toMembership(StudentEnrolled event);
}
