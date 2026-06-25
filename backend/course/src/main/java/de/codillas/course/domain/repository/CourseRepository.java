package de.codillas.course.domain.repository;

import de.codillas.course.domain.model.Course;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {}
