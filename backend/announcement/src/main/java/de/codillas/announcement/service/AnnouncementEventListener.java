package de.codillas.announcement.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.event.StudentEnrolled;

import lombok.RequiredArgsConstructor;

/** Feeds the local group roster and purges group-scoped rows (durable async delivery). */
@Component
@RequiredArgsConstructor
class AnnouncementEventListener {

  private final AnnouncementService service;

  @ApplicationModuleListener
  void on(StudentEnrolled event) {
    service.onStudentEnrolled(event);
  }

  @ApplicationModuleListener
  void on(GroupDeleted event) {
    service.onGroupDeleted(event);
  }
}
