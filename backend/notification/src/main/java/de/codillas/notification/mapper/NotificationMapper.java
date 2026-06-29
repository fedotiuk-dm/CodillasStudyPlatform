package de.codillas.notification.mapper;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;

import de.codillas.notification.api.dto.NotificationListResponse;
import de.codillas.notification.api.dto.NotificationResponse;
import de.codillas.notification.domain.model.Notification;
import de.codillas.notification.domain.model.NotificationMembership;
import de.codillas.notification.domain.model.NotificationType;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface NotificationMapper {

  NotificationResponse toResponse(Notification notification);

  NotificationListResponse toListResponse(Page<Notification> page, long unread);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Notification toNotification(
      UUID recipientId,
      NotificationType type,
      String title,
      String body,
      Map<String, String> params,
      UUID referenceId);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "studentId", source = "userId")
  NotificationMembership toMembership(StudentEnrolled event);
}
