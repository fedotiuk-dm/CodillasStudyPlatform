package de.codillas.enrollment.service;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.enrollment.api.dto.GroupResponse;
import de.codillas.enrollment.api.dto.ScheduledLessonResponse;
import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.model.GroupStatus;
import de.codillas.enrollment.domain.model.Membership;
import de.codillas.enrollment.domain.repository.GroupRepository;
import de.codillas.enrollment.domain.repository.MembershipRepository;
import de.codillas.enrollment.domain.repository.ScheduledLessonRepository;
import de.codillas.enrollment.mapper.GroupMapper;
import de.codillas.enrollment.mapper.ScheduledLessonMapper;
import de.codillas.shared.security.CurrentUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Current-user-scoped views: the groups I belong to or teach, and their lesson schedule. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MyEnrollmentServiceImpl implements MyEnrollmentService {

  private final MembershipRepository membershipRepository;
  private final GroupRepository groupRepository;
  private final ScheduledLessonRepository scheduledLessonRepository;
  private final GroupMapper groupMapper;
  private final ScheduledLessonMapper scheduledLessonMapper;
  private final CurrentUser currentUser;

  @Override
  public List<GroupResponse> listMyGroups() {
    List<UUID> groupIds = myGroupIds();
    if (groupIds.isEmpty()) {
      return List.of();
    }
    return groupMapper.toResponseList(
        groupRepository.findByIdInAndStatusIn(
            groupIds,
            EnumSet.of(GroupStatus.RUNNING, GroupStatus.ARCHIVED),
            GroupRepository.BY_NAME));
  }

  @Override
  public List<ScheduledLessonResponse> listMySchedule() {
    List<UUID> groupIds = myGroupIds();
    if (groupIds.isEmpty()) {
      return List.of();
    }
    return scheduledLessonMapper.toResponseList(
        scheduledLessonRepository.findByGroupIdIn(groupIds, ScheduledLessonRepository.BY_TIME));
  }

  private List<UUID> myGroupIds() {
    UUID userId = currentUser.id();
    return Stream.concat(
            membershipRepository.findByUserId(userId).stream().map(Membership::getGroupId),
            groupRepository.findByTeacherId(userId).stream().map(Group::getId))
        .distinct()
        .toList();
  }
}
