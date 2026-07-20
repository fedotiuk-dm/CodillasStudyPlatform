package de.codillas.homework.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.homework.api.dto.AssignmentListResponse;
import de.codillas.homework.api.dto.AssignmentResponse;
import de.codillas.homework.api.dto.CreateAssignmentRequest;
import de.codillas.homework.domain.AssignmentStateMachine;
import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.AssignmentStatus;
import de.codillas.homework.domain.repository.AssignmentRepository;
import de.codillas.homework.mapper.AssignmentMapper;
import de.codillas.shared.event.AssignmentPublished;
import de.codillas.shared.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentServiceImpl implements AssignmentService {

  private final AssignmentRepository repository;
  private final AssignmentMapper mapper;
  private final AssignmentStateMachine stateMachine;
  private final ApplicationEventPublisher events;

  @Override
  @Transactional
  public AssignmentResponse createAssignment(CreateAssignmentRequest request) {
    return mapper.toResponse(repository.save(mapper.toEntity(request)));
  }

  @Override
  @Transactional
  public AssignmentResponse publishAssignment(UUID assignmentId) {
    Assignment assignment = findByIdOrThrow(assignmentId);
    stateMachine.transitionTo(assignment, AssignmentStatus.PUBLISHED);
    Assignment saved = repository.save(assignment);
    events.publishEvent(new AssignmentPublished(saved.getId(), saved.getGroupId()));
    return mapper.toResponse(saved);
  }

  @Override
  public AssignmentListResponse listAssignments(UUID groupId, Pageable pageable) {
    return mapper.toListResponse(repository.findByGroupId(groupId, pageable));
  }

  /**
   * A deleted lesson does not delete the assignment — it belongs to a group and keeps its own
   * brief, deadline and submissions; only the course-content back-pointer is cleared.
   */
  @Override
  @Transactional
  public void onLessonsDeleted(List<UUID> lessonIds) {
    List<Assignment> affected = repository.findByLessonIdIn(lessonIds);
    affected.forEach(assignment -> assignment.setLessonId(null));
    repository.saveAll(affected);
  }

  private Assignment findByIdOrThrow(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("Assignment", id));
  }
}
