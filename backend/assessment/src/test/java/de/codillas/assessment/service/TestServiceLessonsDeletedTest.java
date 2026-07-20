package de.codillas.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import de.codillas.assessment.domain.model.Test;
import de.codillas.assessment.domain.repository.TestRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestServiceImpl — LessonsDeleted")
class TestServiceLessonsDeletedTest {

  @Mock private TestRepository repository;
  @InjectMocks private TestServiceImpl service;

  @org.junit.jupiter.api.Test
  @DisplayName("clears lessonId but keeps the test, so attempts and their grades survive")
  void clearsLessonIdWithoutDeleting() {
    UUID lessonId = UUID.randomUUID();
    when(repository.findByLessonIdIn(List.of(lessonId)))
        .thenReturn(List.of(Test.builder().lessonId(lessonId).build()));

    service.onLessonsDeleted(List.of(lessonId));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Test>> saved = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(saved.capture());
    assertThat(saved.getValue()).singleElement().extracting(Test::getLessonId).isNull();
  }
}
