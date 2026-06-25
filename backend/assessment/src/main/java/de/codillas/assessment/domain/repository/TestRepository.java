package de.codillas.assessment.domain.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.assessment.domain.model.Test;

@Repository
public interface TestRepository extends JpaRepository<Test, UUID> {

  Page<Test> findByLessonId(UUID lessonId, Pageable pageable);
}
