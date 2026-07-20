package de.codillas.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.course.api.dto.CreateMaterialRequest;
import de.codillas.course.api.dto.MaterialType;
import de.codillas.course.domain.model.Lesson;
import de.codillas.course.domain.model.Section;
import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.course.domain.repository.LessonRepository;
import de.codillas.course.domain.repository.MaterialRepository;
import de.codillas.course.domain.repository.SectionRepository;
import de.codillas.course.mapper.CourseMapper;
import de.codillas.course.mapper.LessonMapper;
import de.codillas.shared.event.LessonsDeleted;
import de.codillas.shared.exception.BadRequestException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseService — structure authoring")
class CourseStructureServiceTest {

  @Mock private CourseRepository courseRepository;
  @Mock private SectionRepository sectionRepository;
  @Mock private LessonRepository lessonRepository;
  @Mock private MaterialRepository materialRepository;
  @Mock private CourseMapper courseMapper;
  @Mock private LessonMapper lessonMapper;
  @Mock private ApplicationEventPublisher events;
  @InjectMocks private CourseServiceImpl service;

  @Test
  @DisplayName("addMaterial rejects a FILE material with no fileId")
  void addMaterial_fileWithoutFileId_isRejected() {
    UUID lessonId = UUID.randomUUID();
    when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(new Lesson()));
    CreateMaterialRequest request = new CreateMaterialRequest(MaterialType.FILE, "Slides");

    assertThatThrownBy(() -> service.addMaterial(lessonId, request))
        .isInstanceOf(BadRequestException.class);
    verify(materialRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("addMaterial rejects a LINK material with no url")
  void addMaterial_linkWithoutUrl_isRejected() {
    UUID lessonId = UUID.randomUUID();
    when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(new Lesson()));
    CreateMaterialRequest request = new CreateMaterialRequest(MaterialType.LINK, "Docs");

    assertThatThrownBy(() -> service.addMaterial(lessonId, request))
        .isInstanceOf(BadRequestException.class);
    verify(materialRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName(
      "deleteSection cascades: removes the section's lessons' materials, then lessons, then the section")
  void deleteSection_cascadesToLessonsAndMaterials() {
    UUID sectionId = UUID.randomUUID();
    UUID lessonA = UUID.randomUUID();
    UUID lessonB = UUID.randomUUID();
    Section section = Section.builder().id(sectionId).build();
    when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
    when(lessonRepository.findBySectionId(sectionId, LessonRepository.BY_ORDER))
        .thenReturn(
            List.of(Lesson.builder().id(lessonA).build(), Lesson.builder().id(lessonB).build()));

    service.deleteSection(sectionId);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<UUID>> ids = ArgumentCaptor.forClass(List.class);
    verify(materialRepository).deleteByLessonIdIn(ids.capture());
    assertThat(ids.getValue()).containsExactlyInAnyOrder(lessonA, lessonB);
    verify(lessonRepository).deleteBySectionId(sectionId);
    verify(sectionRepository).delete(section);
    // Downstream modules (enrollment/homework/assessment) hold a lessonId; without this event
    // they would keep pointing at rows that no longer exist.
    verify(events).publishEvent(new LessonsDeleted(List.of(lessonA, lessonB)));
  }
}
