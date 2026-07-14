package de.codillas.course.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.model.CourseStatus;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

  Page<Course> findByStatus(CourseStatus status, Pageable pageable);

  Page<Course> findByStatusIn(Collection<CourseStatus> statuses, Pageable pageable);

  List<Course> findByStatus(CourseStatus status);
}
