package de.codillas.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.enrollment.domain.GroupStateMachine;
import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.model.GroupStatus;
import de.codillas.enrollment.domain.repository.GroupRepository;
import de.codillas.enrollment.mapper.GroupMapper;
import de.codillas.shared.event.CourseArchived;
import de.codillas.shared.event.GroupArchived;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupServiceImpl — CourseArchived cascade")
class GroupServiceArchiveCascadeTest {

  @Mock private GroupRepository repository;
  @Mock private GroupMapper mapper;
  @Mock private ApplicationEventPublisher events;

  @org.mockito.Spy private GroupStateMachine stateMachine = new GroupStateMachine();

  @InjectMocks private GroupServiceImpl service;

  @Test
  @DisplayName("retires the running cohorts and announces each one")
  void archivesRunningGroups() {
    UUID courseId = UUID.randomUUID();
    UUID runningId = UUID.randomUUID();
    Group running = Group.builder().id(runningId).status(GroupStatus.RUNNING).build();
    when(repository.findByCourseId(courseId)).thenReturn(List.of(running));
    when(repository.findById(runningId)).thenReturn(Optional.of(running));
    when(repository.save(running)).thenReturn(running);

    service.onCourseArchived(new CourseArchived(courseId));

    assertThat(running.getStatus()).isEqualTo(GroupStatus.ARCHIVED);
    verify(events).publishEvent(new GroupArchived(runningId));
  }

  @Test
  @DisplayName("skips cohorts that are already archived — the transition is terminal")
  void skipsArchivedGroups() {
    UUID courseId = UUID.randomUUID();
    Group archived = Group.builder().id(UUID.randomUUID()).status(GroupStatus.ARCHIVED).build();
    when(repository.findByCourseId(courseId)).thenReturn(List.of(archived));

    service.onCourseArchived(new CourseArchived(courseId));

    verify(repository, never()).save(any());
    verify(events, never()).publishEvent(any(GroupArchived.class));
  }
}
