package de.codillas.enrollment.service;

import java.util.List;
import java.util.UUID;

import de.codillas.enrollment.api.dto.AttendanceResponse;
import de.codillas.enrollment.api.dto.MarkAttendanceRequest;

public interface AttendanceService {

  AttendanceResponse markAttendance(UUID scheduledLessonId, MarkAttendanceRequest request);

  List<AttendanceResponse> listAttendance(UUID scheduledLessonId);
}
