package de.codillas.homework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import de.codillas.homework.api.dto.AssignmentListResponse;
import de.codillas.homework.api.dto.AssignmentResponse;
import de.codillas.homework.api.dto.CreateAssignmentRequest;
import de.codillas.homework.domain.AssignmentStateMachine;
import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.AssignmentStatus;
import de.codillas.homework.domain.repository.AssignmentRepository;
import de.codillas.homework.mapper.AssignmentMapper;
import de.codillas.shared.event.AssignmentPublished;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentService")
class AssignmentServiceTest {

  @Mock private AssignmentRepository repository;
  @Mock private AssignmentMapper mapper;
  @Mock private AssignmentStateMachine stateMachine;
  @Mock private ApplicationEventPublisher events;
  @InjectMocks private AssignmentServiceImpl service;

  @Test
  @DisplayName("createAssignment maps the request, saves it and returns the response")
  void createAssignment_savesAndReturns() {
    CreateAssignmentRequest request = mock(CreateAssignmentRequest.class);
    Assignment toSave = Assignment.builder().title("HW1").build();
    Assignment saved = Assignment.builder().title("HW1").build();
    AssignmentResponse dto = mock(AssignmentResponse.class);
    when(mapper.toEntity(request)).thenReturn(toSave);
    when(repository.save(toSave)).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(dto);

    assertThat(service.createAssignment(request)).isSameAs(dto);
  }

  @Test
  @DisplayName(
      "publishAssignment transitions to PUBLISHED, saves and publishes AssignmentPublished")
  void publishAssignment_transitionsAndPublishesEvent() {
    UUID assignmentId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    Assignment assignment =
        Assignment.builder().id(assignmentId).groupId(groupId).title("HW1").build();
    AssignmentResponse dto = mock(AssignmentResponse.class);
    when(repository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(repository.save(assignment)).thenReturn(assignment);
    when(mapper.toResponse(assignment)).thenReturn(dto);

    assertThat(service.publishAssignment(assignmentId)).isSameAs(dto);
    verify(stateMachine).transitionTo(assignment, AssignmentStatus.PUBLISHED);
    verify(events).publishEvent(new AssignmentPublished(assignmentId, groupId));
  }

  @Test
  @DisplayName("listAssignments maps the page for the group")
  void listAssignments_mapsPage() {
    UUID groupId = UUID.randomUUID();
    Pageable pageable = Pageable.unpaged();
    AssignmentListResponse dto = mock(AssignmentListResponse.class);
    when(repository.findByGroupId(groupId, pageable))
        .thenReturn(org.springframework.data.domain.Page.empty());
    when(mapper.toListResponse(org.springframework.data.domain.Page.empty())).thenReturn(dto);

    assertThat(service.listAssignments(groupId, pageable)).isSameAs(dto);
  }
}
