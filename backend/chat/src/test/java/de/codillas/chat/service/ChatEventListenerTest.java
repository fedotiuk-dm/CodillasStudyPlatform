package de.codillas.chat.service;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import de.codillas.shared.event.GroupCreated;
import de.codillas.shared.event.StudentEnrolled;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatEventListener")
class ChatEventListenerTest {

  @Mock private ChatService service;
  @InjectMocks private ChatEventListener listener;

  @Test
  @DisplayName("StudentEnrolled adds the student to the group channel")
  void delegatesEnrolment() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    listener.on(new StudentEnrolled(groupId, userId));
    verify(service).onStudentEnrolled(groupId, userId);
  }

  @Test
  @DisplayName("GroupCreated seats the teacher in the group channel")
  void delegatesGroupCreated() {
    UUID groupId = UUID.randomUUID();
    UUID teacherId = UUID.randomUUID();
    listener.on(new GroupCreated(groupId, teacherId));
    verify(service).onGroupCreated(groupId, teacherId);
  }
}
