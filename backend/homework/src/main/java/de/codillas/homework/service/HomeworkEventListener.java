package de.codillas.homework.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.GroupDeleted;

import lombok.RequiredArgsConstructor;

/** Purges a deleted group's assignments and their submissions/reviews/grades. */
@Component
@RequiredArgsConstructor
class HomeworkEventListener {
  private final SubmissionService service;

  @ApplicationModuleListener
  void on(GroupDeleted event) {
    service.onGroupDeleted(event.groupId());
  }
}
