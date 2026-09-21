package de.codillas.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import de.codillas.enrollment.api.dto.CreateGroupRequest;
import de.codillas.enrollment.api.dto.GroupListResponse;
import de.codillas.enrollment.api.dto.GroupResponse;
import de.codillas.enrollment.api.dto.GroupStatus;
import de.codillas.enrollment.domain.GroupStateMachine;
import de.codillas.enrollment.domain.model.CourseStatusView;
import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.repository.CourseStatusViewRepository;
import de.codillas.enrollment.domain.repository.GroupRepository;
import de.codillas.enrollment.mapper.GroupMapper;
import de.codillas.shared.event.GroupCreated;
import de.codillas.shared.exception.ConflictException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupService")
class GroupServiceTest {

  @Mock private GroupRepository repository;
  @Mock private CourseStatusViewRepository courseStatusRepository;
  @Mock private GroupMapper mapper;
  @Mock private ApplicationEventPublisher events;
  @Mock private GroupStateMachine stateMachine;
  @InjectMocks private GroupServiceImpl service;

  private static CreateGroupRequest requestForCourse(UUID courseId) {
    return new CreateGroupRequest("Cohort A", courseId, UUID.randomUUID());
  }

  @Test
  @DisplayName("createGroup against a PUBLISHED course maps, persists and returns the response")
  void createGroup_publishedCourse_mapsPersistsAndReturns() {
    UUID courseId = UUID.randomUUID();
    CreateGroupRequest request = requestForCourse(courseId);
    UUID teacherId = UUID.randomUUID();
    Group toSave = Group.builder().name("Cohort A").build();
    Group saved =
        Group.builder().id(UUID.randomUUID()).name("Cohort A").teacherId(teacherId).build();
    GroupResponse response =
        new GroupResponse(saved.getId(), "Cohort A", courseId, teacherId, GroupStatus.DRAFT);

    when(courseStatusRepository.findById(courseId))
        .thenReturn(Optional.of(new CourseStatusView(courseId, "PUBLISHED")));
    when(mapper.toEntity(request)).thenReturn(toSave);
    when(repository.save(toSave)).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(response);

    assertThat(service.createGroup(request)).isSameAs(response);
    verify(repository).save(toSave);
    verify(events).publishEvent(new GroupCreated(saved.getId(), teacherId));
  }

  @Test
  @DisplayName("createGroup against a course missing from the read model is a 409 conflict")
  void createGroup_unknownCourse_conflicts() {
    UUID courseId = UUID.randomUUID();
    when(courseStatusRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> service.createGroup(requestForCourse(courseId)));
    verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("createGroup against an ARCHIVED course is a 409 conflict")
  void createGroup_archivedCourse_conflicts() {
    UUID courseId = UUID.randomUUID();
    when(courseStatusRepository.findById(courseId))
        .thenReturn(Optional.of(new CourseStatusView(courseId, "ARCHIVED")));

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> service.createGroup(requestForCourse(courseId)));
    verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("listGroups maps the repository page to the list response")
  void listGroups_returnsMappedPage() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Group> page = new PageImpl<>(List.of(Group.builder().name("Cohort A").build()));
    GroupListResponse expected = mock(GroupListResponse.class);

    when(repository.findAll(pageable)).thenReturn(page);
    when(mapper.toListResponse(page)).thenReturn(expected);

    assertThat(service.listGroups(null, pageable)).isSameAs(expected);
  }
}
