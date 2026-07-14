package de.codillas.enrollment.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.repository.GroupRepository;
import de.codillas.enrollment.mapper.GroupMapper;
import de.codillas.shared.event.CourseDeleted;
import de.codillas.shared.event.GroupDeleted;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupServiceImpl — delete + CourseDeleted cascade")
class GroupServiceDeleteTest {
  @Mock GroupRepository repository;
  @Mock GroupMapper mapper;
  @Mock ApplicationEventPublisher events;
  @InjectMocks GroupServiceImpl service;

  @Test
  @DisplayName("deletes the group and publishes GroupDeleted")
  void deletesAndPublishes() {
    UUID id = UUID.randomUUID();
    Group group = Group.builder().build();
    when(repository.findById(id)).thenReturn(Optional.of(group));

    service.deleteGroup(id);

    verify(repository).delete(group);
    verify(events).publishEvent(new GroupDeleted(id));
  }

  @Test
  @DisplayName("CourseDeleted cascades to every group of that course")
  void courseDeletedCascades() {
    UUID courseId = UUID.randomUUID();
    Group g1 = Group.builder().build();
    Group g2 = Group.builder().build();
    g1.setId(UUID.randomUUID());
    g2.setId(UUID.randomUUID());
    when(repository.findByCourseId(courseId)).thenReturn(List.of(g1, g2));
    when(repository.findById(g1.getId())).thenReturn(Optional.of(g1));
    when(repository.findById(g2.getId())).thenReturn(Optional.of(g2));

    service.onCourseDeleted(new CourseDeleted(courseId));

    verify(events).publishEvent(new GroupDeleted(g1.getId()));
    verify(events).publishEvent(new GroupDeleted(g2.getId()));
  }
}
