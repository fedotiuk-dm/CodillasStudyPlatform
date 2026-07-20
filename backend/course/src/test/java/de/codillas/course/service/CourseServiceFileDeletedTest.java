package de.codillas.course.service;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.course.domain.repository.LessonRepository;
import de.codillas.course.domain.repository.MaterialRepository;
import de.codillas.course.domain.repository.SectionRepository;
import de.codillas.course.mapper.CourseMapper;
import de.codillas.course.mapper.LessonMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseServiceImpl — FileDeleted")
class CourseServiceFileDeletedTest {

  @Mock CourseRepository courseRepository;
  @Mock SectionRepository sectionRepository;
  @Mock LessonRepository lessonRepository;
  @Mock MaterialRepository materialRepository;
  @Mock CourseMapper courseMapper;
  @Mock LessonMapper lessonMapper;
  @Mock ApplicationEventPublisher events;
  @InjectMocks CourseServiceImpl service;

  @Test
  @DisplayName("drops the materials that pointed at the deleted file")
  void dropsOrphanedMaterials() {
    UUID fileId = UUID.randomUUID();

    service.onFileDeleted(fileId);

    verify(materialRepository).deleteByFileId(fileId);
  }
}
