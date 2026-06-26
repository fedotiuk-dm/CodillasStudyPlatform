package de.codillas.course.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import de.codillas.course.api.dto.CourseDetailResponse;
import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CreateCourseRequest;
import de.codillas.course.api.dto.CreateLessonRequest;
import de.codillas.course.api.dto.CreateMaterialRequest;
import de.codillas.course.api.dto.CreateSectionRequest;
import de.codillas.course.api.dto.LessonResponse;
import de.codillas.course.api.dto.MaterialResponse;
import de.codillas.course.api.dto.SectionResponse;
import de.codillas.course.api.dto.UpdateLessonRequest;
import de.codillas.course.api.dto.UpdateSectionRequest;

public interface CourseService {

  CourseResponse createCourse(CreateCourseRequest request);

  CourseListResponse listCourses(Pageable pageable);

  CourseDetailResponse getCourse(UUID courseId);

  SectionResponse createSection(UUID courseId, CreateSectionRequest request);

  SectionResponse updateSection(UUID sectionId, UpdateSectionRequest request);

  void deleteSection(UUID sectionId);

  LessonResponse createLesson(UUID sectionId, CreateLessonRequest request);

  LessonResponse getLesson(UUID lessonId);

  LessonResponse updateLesson(UUID lessonId, UpdateLessonRequest request);

  void deleteLesson(UUID lessonId);

  MaterialResponse addMaterial(UUID lessonId, CreateMaterialRequest request);

  void deleteMaterial(UUID materialId);
}
