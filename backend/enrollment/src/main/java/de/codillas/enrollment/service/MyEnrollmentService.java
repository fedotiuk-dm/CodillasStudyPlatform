package de.codillas.enrollment.service;

import java.util.List;

import de.codillas.enrollment.api.dto.GroupResponse;
import de.codillas.enrollment.api.dto.ScheduledLessonResponse;

/** Current-user-scoped enrollment views: the groups I belong to and my lesson schedule. */
public interface MyEnrollmentService {

  List<GroupResponse> listMyGroups();

  List<ScheduledLessonResponse> listMySchedule();
}
