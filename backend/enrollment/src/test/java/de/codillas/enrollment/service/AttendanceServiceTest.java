package de.codillas.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import de.codillas.enrollment.api.dto.AttendanceResponse;
import de.codillas.enrollment.api.dto.MarkAttendanceRequest;
import de.codillas.enrollment.domain.model.Attendance;
import de.codillas.enrollment.domain.repository.AttendanceRepository;
import de.codillas.enrollment.mapper.AttendanceMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService")
class AttendanceServiceTest {

  @Mock private AttendanceRepository repository;
  @Mock private AttendanceMapper mapper;
  @InjectMocks private AttendanceServiceImpl service;

  @Test
  @DisplayName("markAttendance creates a new record when none exists")
  void markAttendance_whenNew_createsRecord() {
    UUID lessonId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    MarkAttendanceRequest request = new MarkAttendanceRequest(userId, true);
    Attendance created = Attendance.builder().scheduledLessonId(lessonId).userId(userId).build();
    AttendanceResponse dto = new AttendanceResponse(UUID.randomUUID(), lessonId, userId, true);
    when(repository.findByScheduledLessonIdAndUserId(lessonId, userId))
        .thenReturn(Optional.empty());
    when(mapper.toEntity(request, lessonId)).thenReturn(created);
    when(repository.save(created)).thenReturn(created);
    when(mapper.toResponse(created)).thenReturn(dto);

    assertThat(service.markAttendance(lessonId, request)).isSameAs(dto);
    verify(mapper).updateEntity(created, request);
  }

  @Test
  @DisplayName("markAttendance updates the existing record on re-mark")
  void markAttendance_whenExisting_updatesRecord() {
    UUID lessonId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    MarkAttendanceRequest request = new MarkAttendanceRequest(userId, false);
    Attendance existing = Attendance.builder().scheduledLessonId(lessonId).userId(userId).build();
    AttendanceResponse dto = new AttendanceResponse(UUID.randomUUID(), lessonId, userId, false);
    when(repository.findByScheduledLessonIdAndUserId(lessonId, userId))
        .thenReturn(Optional.of(existing));
    when(repository.save(existing)).thenReturn(existing);
    when(mapper.toResponse(existing)).thenReturn(dto);

    assertThat(service.markAttendance(lessonId, request)).isSameAs(dto);
    verify(mapper).updateEntity(existing, request);
    verify(mapper, never()).toEntity(request, lessonId);
  }
}
