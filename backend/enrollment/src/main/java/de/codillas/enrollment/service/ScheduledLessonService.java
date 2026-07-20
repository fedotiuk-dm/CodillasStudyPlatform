package de.codillas.enrollment.service;

import java.util.List;
import java.util.UUID;

import de.codillas.enrollment.api.dto.ScheduleLessonRequest;
import de.codillas.enrollment.api.dto.ScheduledLessonResponse;

public interface ScheduledLessonService {

  ScheduledLessonResponse scheduleLesson(UUID groupId, ScheduleLessonRequest request);

  List<ScheduledLessonResponse> listScheduledLessons(UUID groupId);

  void onLessonsDeleted(List<UUID> lessonIds);
}
