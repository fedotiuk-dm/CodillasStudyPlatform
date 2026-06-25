package de.codillas.enrollment.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.enrollment.api.dto.ScheduleLessonRequest;
import de.codillas.enrollment.api.dto.ScheduledLessonResponse;
import de.codillas.enrollment.domain.repository.ScheduledLessonRepository;
import de.codillas.enrollment.mapper.ScheduledLessonMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduledLessonServiceImpl implements ScheduledLessonService {

  private final ScheduledLessonRepository repository;
  private final ScheduledLessonMapper mapper;

  @Override
  @Transactional
  public ScheduledLessonResponse scheduleLesson(UUID groupId, ScheduleLessonRequest request) {
    return mapper.toResponse(repository.save(mapper.toEntity(request, groupId)));
  }

  @Override
  public List<ScheduledLessonResponse> listScheduledLessons(UUID groupId) {
    return mapper.toResponseList(
        repository.findByGroupId(groupId, ScheduledLessonRepository.BY_TIME));
  }
}
