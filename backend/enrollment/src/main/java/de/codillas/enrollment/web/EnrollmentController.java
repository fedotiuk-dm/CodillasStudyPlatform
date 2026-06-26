package de.codillas.enrollment.web;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import de.codillas.enrollment.api.EnrollmentApi;
import de.codillas.enrollment.api.dto.AttendanceResponse;
import de.codillas.enrollment.api.dto.CreateGroupRequest;
import de.codillas.enrollment.api.dto.EnrollStudentRequest;
import de.codillas.enrollment.api.dto.GroupListResponse;
import de.codillas.enrollment.api.dto.GroupResponse;
import de.codillas.enrollment.api.dto.MarkAttendanceRequest;
import de.codillas.enrollment.api.dto.MembershipResponse;
import de.codillas.enrollment.api.dto.ScheduleLessonRequest;
import de.codillas.enrollment.api.dto.ScheduledLessonResponse;
import de.codillas.enrollment.service.AttendanceService;
import de.codillas.enrollment.service.GroupService;
import de.codillas.enrollment.service.MeService;
import de.codillas.enrollment.service.MembershipService;
import de.codillas.enrollment.service.ScheduledLessonService;
import de.codillas.shared.security.RequiresAdmin;
import de.codillas.shared.security.RequiresAuthenticated;

import lombok.RequiredArgsConstructor;

/** Thin delegator — implements the generated {@link EnrollmentApi}. */
@RestController
@RequiredArgsConstructor
public class EnrollmentController implements EnrollmentApi {

  private final GroupService groupService;
  private final MembershipService membershipService;
  private final ScheduledLessonService scheduledLessonService;
  private final AttendanceService attendanceService;
  private final MeService meService;

  @Override
  @RequiresAdmin
  public ResponseEntity<GroupResponse> createGroup(CreateGroupRequest createGroupRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(groupService.createGroup(createGroupRequest));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<GroupListResponse> listGroups(Pageable pageable) {
    return ResponseEntity.ok(groupService.listGroups(pageable));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<List<GroupResponse>> listMyGroups() {
    return ResponseEntity.ok(meService.listMyGroups());
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<List<ScheduledLessonResponse>> listMySchedule() {
    return ResponseEntity.ok(meService.listMySchedule());
  }

  @Override
  @RequiresAdmin
  public ResponseEntity<MembershipResponse> enrollStudent(
      UUID groupId, EnrollStudentRequest enrollStudentRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(membershipService.enrollStudent(groupId, enrollStudentRequest));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<List<MembershipResponse>> listGroupMembers(UUID groupId) {
    return ResponseEntity.ok(membershipService.listGroupMembers(groupId));
  }

  @Override
  @RequiresAdmin
  public ResponseEntity<ScheduledLessonResponse> scheduleLesson(
      UUID groupId, ScheduleLessonRequest scheduleLessonRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(scheduledLessonService.scheduleLesson(groupId, scheduleLessonRequest));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<List<ScheduledLessonResponse>> listScheduledLessons(UUID groupId) {
    return ResponseEntity.ok(scheduledLessonService.listScheduledLessons(groupId));
  }

  @Override
  @RequiresAdmin
  public ResponseEntity<AttendanceResponse> markAttendance(
      UUID groupId, UUID scheduledLessonId, MarkAttendanceRequest markAttendanceRequest) {
    return ResponseEntity.ok(
        attendanceService.markAttendance(scheduledLessonId, markAttendanceRequest));
  }

  @Override
  @RequiresAdmin
  public ResponseEntity<List<AttendanceResponse>> listAttendance(
      UUID groupId, UUID scheduledLessonId) {
    return ResponseEntity.ok(attendanceService.listAttendance(scheduledLessonId));
  }
}
