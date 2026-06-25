package de.codillas.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import de.codillas.enrollment.api.dto.ScheduleLessonRequest;
import de.codillas.enrollment.api.dto.ScheduledLessonResponse;
import de.codillas.enrollment.domain.model.ScheduledLesson;
import de.codillas.enrollment.domain.repository.ScheduledLessonRepository;
import de.codillas.enrollment.mapper.ScheduledLessonMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledLessonService")
class ScheduledLessonServiceTest {

  @Mock private ScheduledLessonRepository repository;
  @Mock private ScheduledLessonMapper mapper;
  @InjectMocks private ScheduledLessonServiceImpl service;

  @Test
  @DisplayName("scheduleLesson maps the request, persists it, and returns the response")
  void scheduleLesson_mapsPersistsAndReturns() {
    UUID groupId = UUID.randomUUID();
    ScheduleLessonRequest request = new ScheduleLessonRequest("Intro", Instant.now());
    ScheduledLesson toSave = ScheduledLesson.builder().groupId(groupId).title("Intro").build();
    ScheduledLesson saved = ScheduledLesson.builder().groupId(groupId).title("Intro").build();
    ScheduledLessonResponse dto =
        new ScheduledLessonResponse(UUID.randomUUID(), groupId, "Intro", Instant.now());
    when(mapper.toEntity(request, groupId)).thenReturn(toSave);
    when(repository.save(toSave)).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(dto);

    assertThat(service.scheduleLesson(groupId, request)).isSameAs(dto);
  }

  @Test
  @DisplayName("listScheduledLessons maps the group's lessons")
  void listScheduledLessons_mapsLessons() {
    UUID groupId = UUID.randomUUID();
    List<ScheduledLesson> lessons = List.of(ScheduledLesson.builder().groupId(groupId).build());
    List<ScheduledLessonResponse> dtos = List.of(mock(ScheduledLessonResponse.class));
    when(repository.findByGroupId(groupId, ScheduledLessonRepository.BY_TIME)).thenReturn(lessons);
    when(mapper.toResponseList(lessons)).thenReturn(dtos);

    assertThat(service.listScheduledLessons(groupId)).isSameAs(dtos);
  }
}
