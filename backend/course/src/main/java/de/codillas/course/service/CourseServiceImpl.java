package de.codillas.course.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.course.api.dto.CourseDetailResponse;
import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CreateCourseRequest;
import de.codillas.course.api.dto.CreateLessonRequest;
import de.codillas.course.api.dto.CreateMaterialRequest;
import de.codillas.course.api.dto.CreateSectionRequest;
import de.codillas.course.api.dto.LessonResponse;
import de.codillas.course.api.dto.MaterialResponse;
import de.codillas.course.api.dto.MaterialType;
import de.codillas.course.api.dto.SectionResponse;
import de.codillas.course.api.dto.UpdateLessonRequest;
import de.codillas.course.api.dto.UpdateSectionRequest;
import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.model.Lesson;
import de.codillas.course.domain.model.Material;
import de.codillas.course.domain.model.Section;
import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.course.domain.repository.LessonRepository;
import de.codillas.course.domain.repository.MaterialRepository;
import de.codillas.course.domain.repository.SectionRepository;
import de.codillas.course.mapper.CourseMapper;
import de.codillas.course.mapper.LessonMapper;
import de.codillas.shared.exception.BadRequestException;
import de.codillas.shared.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

  private final CourseRepository courseRepository;
  private final SectionRepository sectionRepository;
  private final LessonRepository lessonRepository;
  private final MaterialRepository materialRepository;
  private final CourseMapper courseMapper;
  private final LessonMapper lessonMapper;

  @Override
  @Transactional
  public CourseResponse createCourse(CreateCourseRequest request) {
    return courseMapper.toResponse(courseRepository.save(courseMapper.toEntity(request)));
  }

  @Override
  public CourseListResponse listCourses(Pageable pageable) {
    return courseMapper.toListResponse(courseRepository.findAll(pageable));
  }

  @Override
  public CourseDetailResponse getCourse(UUID courseId) {
    Course course = findCourseOrThrow(courseId);
    List<Section> sections = sectionRepository.findByCourseId(courseId, SectionRepository.BY_ORDER);
    Map<UUID, List<Lesson>> lessonsBySection =
        sections.isEmpty()
            ? Map.of()
            : lessonRepository
                .findBySectionIdIn(
                    sections.stream().map(Section::getId).toList(), LessonRepository.BY_ORDER)
                .stream()
                .collect(Collectors.groupingBy(Lesson::getSectionId));

    List<SectionResponse> sectionResponses =
        sections.stream()
            .map(
                section ->
                    sectionResponse(
                        section, lessonsBySection.getOrDefault(section.getId(), List.of())))
            .toList();
    return courseMapper.toDetailResponse(course, sectionResponses);
  }

  @Override
  @Transactional
  public SectionResponse createSection(UUID courseId, CreateSectionRequest request) {
    findCourseOrThrow(courseId);
    Section section =
        sectionRepository.save(
            courseMapper.toSectionEntity(request, courseId, sectionRepository.getNextSortOrder()));
    return sectionResponse(section, List.of());
  }

  @Override
  @Transactional
  public SectionResponse updateSection(UUID sectionId, UpdateSectionRequest request) {
    Section section = findSectionOrThrow(sectionId);
    courseMapper.updateSection(section, request);
    Section saved = sectionRepository.save(section);
    return sectionResponse(
        saved, lessonRepository.findBySectionId(sectionId, LessonRepository.BY_ORDER));
  }

  @Override
  @Transactional
  public void deleteSection(UUID sectionId) {
    Section section = findSectionOrThrow(sectionId);
    List<Lesson> lessons = lessonRepository.findBySectionId(sectionId, LessonRepository.BY_ORDER);
    if (!lessons.isEmpty()) {
      materialRepository.deleteByLessonIdIn(lessons.stream().map(Lesson::getId).toList());
      lessonRepository.deleteBySectionId(sectionId);
    }
    sectionRepository.delete(section);
  }

  @Override
  @Transactional
  public LessonResponse createLesson(UUID sectionId, CreateLessonRequest request) {
    findSectionOrThrow(sectionId);
    Lesson lesson =
        lessonRepository.save(
            lessonMapper.toEntity(request, sectionId, lessonRepository.getNextSortOrder()));
    return lessonResponse(lesson);
  }

  @Override
  public LessonResponse getLesson(UUID lessonId) {
    return lessonResponse(findLessonOrThrow(lessonId));
  }

  @Override
  @Transactional
  public LessonResponse updateLesson(UUID lessonId, UpdateLessonRequest request) {
    Lesson lesson = findLessonOrThrow(lessonId);
    lessonMapper.updateLesson(lesson, request);
    return lessonResponse(lessonRepository.save(lesson));
  }

  @Override
  @Transactional
  public void deleteLesson(UUID lessonId) {
    Lesson lesson = findLessonOrThrow(lessonId);
    materialRepository.deleteByLessonId(lessonId);
    lessonRepository.delete(lesson);
  }

  @Override
  @Transactional
  public MaterialResponse addMaterial(UUID lessonId, CreateMaterialRequest request) {
    findLessonOrThrow(lessonId);
    validateMaterial(request);
    Material material =
        materialRepository.save(
            lessonMapper.toMaterialEntity(
                request, lessonId, materialRepository.getNextSortOrder()));
    return lessonMapper.toMaterialResponse(material);
  }

  @Override
  @Transactional
  public void deleteMaterial(UUID materialId) {
    materialRepository.delete(findMaterialOrThrow(materialId));
  }

  private SectionResponse sectionResponse(Section section, List<Lesson> lessons) {
    return courseMapper.toSectionResponse(
        section, lessons.stream().map(courseMapper::toLessonSummary).toList());
  }

  private LessonResponse lessonResponse(Lesson lesson) {
    List<Material> materials =
        materialRepository.findByLessonId(lesson.getId(), MaterialRepository.BY_ORDER);
    return lessonMapper.toResponse(lesson, lessonMapper.toMaterialResponses(materials));
  }

  private void validateMaterial(CreateMaterialRequest request) {
    String url = request.getUrl();
    boolean hasUrl = url != null && !url.isBlank();
    boolean hasFileId = request.getFileId() != null;
    if (request.getType() == MaterialType.FILE) {
      if (!hasFileId) {
        throw new BadRequestException("A FILE material requires a fileId");
      }
      if (hasUrl) {
        throw new BadRequestException("A FILE material must not carry a url");
      }
    } else {
      if (!hasUrl) {
        throw new BadRequestException("A LINK material requires a url");
      }
      if (hasFileId) {
        throw new BadRequestException("A LINK material must not carry a fileId");
      }
    }
  }

  private Course findCourseOrThrow(UUID id) {
    return courseRepository.findById(id).orElseThrow(() -> new NotFoundException("Course", id));
  }

  private Section findSectionOrThrow(UUID id) {
    return sectionRepository.findById(id).orElseThrow(() -> new NotFoundException("Section", id));
  }

  private Lesson findLessonOrThrow(UUID id) {
    return lessonRepository.findById(id).orElseThrow(() -> new NotFoundException("Lesson", id));
  }

  private Material findMaterialOrThrow(UUID id) {
    return materialRepository.findById(id).orElseThrow(() -> new NotFoundException("Material", id));
  }
}
