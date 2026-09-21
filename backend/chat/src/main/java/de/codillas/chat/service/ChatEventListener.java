package de.codillas.chat.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.GroupCreated;
import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.event.StudentEnrolled;

import lombok.RequiredArgsConstructor;

/** Opens a group's chat channel with its teacher, then adds each enrolled student. */
@Component
@RequiredArgsConstructor
class ChatEventListener {

  private final ChatService service;

  @ApplicationModuleListener
  void on(GroupCreated event) {
    service.onGroupCreated(event.groupId(), event.teacherId());
  }

  @ApplicationModuleListener
  void on(StudentEnrolled event) {
    service.onStudentEnrolled(event.groupId(), event.userId());
  }

  @ApplicationModuleListener
  void on(GroupDeleted event) {
    service.onGroupDeleted(event.groupId());
  }
}
