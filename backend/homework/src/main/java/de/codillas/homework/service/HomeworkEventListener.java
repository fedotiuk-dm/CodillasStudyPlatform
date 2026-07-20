package de.codillas.homework.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.event.LessonsDeleted;

import lombok.RequiredArgsConstructor;

/**
 * Purges a deleted group's assignments and their submissions/reviews/grades, and clears the lesson
 * back-pointer of assignments whose lesson was deleted.
 */
@Component
@RequiredArgsConstructor
class HomeworkEventListener {
  private final SubmissionService service;
  private final AssignmentService assignmentService;

  @ApplicationModuleListener
  void on(GroupDeleted event) {
    service.onGroupDeleted(event.groupId());
  }

  @ApplicationModuleListener
  void on(LessonsDeleted event) {
    assignmentService.onLessonsDeleted(event.lessonIds());
  }
}
