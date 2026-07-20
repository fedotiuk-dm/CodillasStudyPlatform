package de.codillas.enrollment.service;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.enrollment.api.dto.CreateGroupRequest;
import de.codillas.enrollment.api.dto.GroupListResponse;
import de.codillas.enrollment.api.dto.GroupResponse;
import de.codillas.enrollment.domain.GroupStateMachine;
import de.codillas.enrollment.domain.model.CourseStatusView;
import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.model.GroupStatus;
import de.codillas.enrollment.domain.repository.CourseStatusViewRepository;
import de.codillas.enrollment.domain.repository.GroupRepository;
import de.codillas.enrollment.mapper.GroupMapper;
import de.codillas.shared.event.CourseDeleted;
import de.codillas.shared.event.GroupArchived;
import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.exception.ConflictException;
import de.codillas.shared.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupServiceImpl implements GroupService {

  private static final String PUBLISHED = "PUBLISHED";

  private final GroupRepository repository;
  private final CourseStatusViewRepository courseStatusRepository;
  private final GroupMapper mapper;
  private final GroupStateMachine stateMachine;
  private final ApplicationEventPublisher events;

  @Override
  @Transactional
  public GroupResponse createGroup(CreateGroupRequest request) {
    // Validate against enrollment's OWN read model (fed by course events) — no course dependency.
    boolean published =
        courseStatusRepository
            .findById(request.getCourseId())
            .map(CourseStatusView::getStatus)
            .map(PUBLISHED::equals)
            .orElse(false);
    if (!published) {
      throw new ConflictException("A group can only be created for a PUBLISHED course");
    }
    return mapper.toResponse(repository.save(mapper.toEntity(request)));
  }

  @Override
  public GroupListResponse listGroups(
      de.codillas.enrollment.api.dto.GroupStatus status, Pageable pageable) {
    return mapper.toListResponse(
        status == null
            ? repository.findAll(pageable)
            : repository.findByStatus(mapper.toDomainStatus(status), pageable));
  }

  @Override
  @Transactional
  public GroupResponse startGroup(UUID groupId) {
    Group group = findByIdOrThrow(groupId);
    stateMachine.transitionTo(group, GroupStatus.RUNNING);
    return mapper.toResponse(repository.save(group));
  }

  @Override
  @Transactional
  public GroupResponse archiveGroup(UUID groupId) {
    Group group = findByIdOrThrow(groupId);
    stateMachine.transitionTo(group, GroupStatus.ARCHIVED);
    GroupResponse response = mapper.toResponse(repository.save(group));
    events.publishEvent(new GroupArchived(groupId));
    return response;
  }

  @Override
  @Transactional
  public void deleteGroup(UUID groupId) {
    Group group = findByIdOrThrow(groupId);
    // memberships/scheduled_lessons/attendance removed by FK ON DELETE CASCADE
    repository.delete(group);
    events.publishEvent(new GroupDeleted(groupId));
  }

  @Override
  @Transactional
  public void onCourseDeleted(CourseDeleted event) {
    repository.findByCourseId(event.courseId()).forEach(group -> deleteGroup(group.getId()));
  }

  private Group findByIdOrThrow(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("Group", id));
  }
}
