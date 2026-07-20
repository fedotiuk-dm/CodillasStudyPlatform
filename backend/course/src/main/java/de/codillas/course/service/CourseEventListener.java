package de.codillas.course.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.FileDeleted;

import lombok.RequiredArgsConstructor;

/** Drops the materials left pointing at a file that was deleted in the files module. */
@Component
@RequiredArgsConstructor
class CourseEventListener {

  private final CourseService service;

  @ApplicationModuleListener
  void on(FileDeleted event) {
    service.onFileDeleted(event.fileId());
  }
}
