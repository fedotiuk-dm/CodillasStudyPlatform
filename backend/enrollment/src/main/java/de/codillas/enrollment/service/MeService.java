package de.codillas.enrollment.service;

import java.util.List;

import de.codillas.enrollment.api.dto.GroupResponse;
import de.codillas.enrollment.api.dto.ScheduledLessonResponse;

public interface MeService {

  List<GroupResponse> listMyGroups();

  List<ScheduledLessonResponse> listMySchedule();
}
