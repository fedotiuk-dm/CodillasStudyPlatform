package de.codillas.chat.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.event.StudentEnrolled;

import lombok.RequiredArgsConstructor;

/** Adds enrolled students to their group's chat channel (creating it on first enrolment). */
@Component
@RequiredArgsConstructor
class ChatEventListener {

  private final ChatService service;

  @ApplicationModuleListener
  void on(StudentEnrolled event) {
    service.onStudentEnrolled(event.groupId(), event.userId());
  }

  @ApplicationModuleListener
  void on(GroupDeleted event) {
    service.onGroupDeleted(event.groupId());
  }
}
