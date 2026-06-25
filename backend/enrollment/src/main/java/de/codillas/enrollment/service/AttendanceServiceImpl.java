package de.codillas.enrollment.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.enrollment.api.dto.AttendanceResponse;
import de.codillas.enrollment.api.dto.MarkAttendanceRequest;
import de.codillas.enrollment.domain.model.Attendance;
import de.codillas.enrollment.domain.repository.AttendanceRepository;
import de.codillas.enrollment.mapper.AttendanceMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

  private final AttendanceRepository repository;
  private final AttendanceMapper mapper;

  /** Upsert: one attendance row per (lesson, student); a re-mark updates {@code present}. */
  @Override
  @Transactional
  public AttendanceResponse markAttendance(UUID scheduledLessonId, MarkAttendanceRequest request) {
    Attendance attendance =
        repository
            .findByScheduledLessonIdAndUserId(scheduledLessonId, request.getUserId())
            .orElseGet(() -> mapper.toEntity(request, scheduledLessonId));
    mapper.updateEntity(attendance, request);
    return mapper.toResponse(repository.save(attendance));
  }
}
