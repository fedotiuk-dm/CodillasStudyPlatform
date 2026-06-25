package de.codillas.course.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.course.domain.model.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {}
