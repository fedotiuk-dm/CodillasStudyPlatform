package de.codillas.enrollment.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.enrollment.api.dto.ScheduleLessonRequest;
import de.codillas.enrollment.api.dto.ScheduledLessonResponse;
import de.codillas.enrollment.domain.GroupStateMachine;
import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.model.ScheduledLesson;
import de.codillas.enrollment.domain.repository.GroupRepository;
import de.codillas.enrollment.domain.repository.ScheduledLessonRepository;
import de.codillas.enrollment.mapper.ScheduledLessonMapper;
import de.codillas.shared.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduledLessonServiceImpl implements ScheduledLessonService {

  private final ScheduledLessonRepository repository;
  private final GroupRepository groupRepository;
  private final GroupStateMachine groupStateMachine;
  private final ScheduledLessonMapper mapper;

  @Override
  @Transactional
  public ScheduledLessonResponse scheduleLesson(UUID groupId, ScheduleLessonRequest request) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException("Group", groupId));
    groupStateMachine.assertWritable(group);
    return mapper.toResponse(repository.save(mapper.toEntity(request, groupId)));
  }

  @Override
  public List<ScheduledLessonResponse> listScheduledLessons(UUID groupId) {
    return mapper.toResponseList(
        repository.findByGroupId(groupId, ScheduledLessonRepository.BY_TIME));
  }

  /**
   * A deleted lesson does not cancel the session — the slot keeps its own title and time, it just
   * stops pointing at course content.
   */
  @Override
  @Transactional
  public void onLessonsDeleted(List<UUID> lessonIds) {
    List<ScheduledLesson> affected = repository.findByLessonIdIn(lessonIds);
    affected.forEach(scheduled -> scheduled.setLessonId(null));
    repository.saveAll(affected);
  }
}
