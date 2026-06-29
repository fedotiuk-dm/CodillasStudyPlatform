package de.codillas.course.web;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import de.codillas.course.api.CourseApi;
import de.codillas.course.api.dto.CourseDetailResponse;
import de.codillas.course.api.dto.CourseListResponse;
import de.codillas.course.api.dto.CourseResponse;
import de.codillas.course.api.dto.CourseStatus;
import de.codillas.course.api.dto.CreateCourseRequest;
import de.codillas.course.api.dto.CreateLessonRequest;
import de.codillas.course.api.dto.CreateMaterialRequest;
import de.codillas.course.api.dto.CreateSectionRequest;
import de.codillas.course.api.dto.LessonResponse;
import de.codillas.course.api.dto.MaterialResponse;
import de.codillas.course.api.dto.SectionResponse;
import de.codillas.course.api.dto.UpdateLessonRequest;
import de.codillas.course.api.dto.UpdateSectionRequest;
import de.codillas.course.service.CourseService;
import de.codillas.shared.security.RequiresAdmin;
import de.codillas.shared.security.RequiresAuthenticated;
import de.codillas.shared.security.RequiresTeacher;

import lombok.RequiredArgsConstructor;

/** Thin delegator — implements the generated {@link CourseApi}, no business logic. */
@RestController
@RequiredArgsConstructor
public class CourseController implements CourseApi {

  private final CourseService service;

  @Override
  @RequiresAdmin
  public ResponseEntity<CourseResponse> createCourse(CreateCourseRequest createCourseRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.createCourse(createCourseRequest));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<CourseListResponse> listCourses(CourseStatus status, Pageable pageable) {
    return ResponseEntity.ok(service.listCourses(status, pageable));
  }

  @Override
  @RequiresAdmin
  public ResponseEntity<CourseResponse> publishCourse(UUID courseId) {
    return ResponseEntity.ok(service.publishCourse(courseId));
  }

  @Override
  @RequiresAdmin
  public ResponseEntity<CourseResponse> archiveCourse(UUID courseId) {
    return ResponseEntity.ok(service.archiveCourse(courseId));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<CourseDetailResponse> getCourse(UUID courseId) {
    return ResponseEntity.ok(service.getCourse(courseId));
  }

  @Override
  @RequiresAdmin
  public ResponseEntity<Void> deleteCourse(UUID courseId) {
    service.deleteCourse(courseId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<SectionResponse> createSection(
      UUID courseId, CreateSectionRequest createSectionRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.createSection(courseId, createSectionRequest));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<SectionResponse> updateSection(
      UUID sectionId, UpdateSectionRequest updateSectionRequest) {
    return ResponseEntity.ok(service.updateSection(sectionId, updateSectionRequest));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<Void> deleteSection(UUID sectionId) {
    service.deleteSection(sectionId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<LessonResponse> createLesson(
      UUID sectionId, CreateLessonRequest createLessonRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.createLesson(sectionId, createLessonRequest));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<LessonResponse> getLesson(UUID lessonId) {
    return ResponseEntity.ok(service.getLesson(lessonId));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<LessonResponse> updateLesson(
      UUID lessonId, UpdateLessonRequest updateLessonRequest) {
    return ResponseEntity.ok(service.updateLesson(lessonId, updateLessonRequest));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<Void> deleteLesson(UUID lessonId) {
    service.deleteLesson(lessonId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<MaterialResponse> addMaterial(
      UUID lessonId, CreateMaterialRequest createMaterialRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.addMaterial(lessonId, createMaterialRequest));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<Void> deleteMaterial(UUID materialId) {
    service.deleteMaterial(materialId);
    return ResponseEntity.noContent().build();
  }
}
