package de.codillas.course.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.course.domain.model.Lesson;
import de.codillas.course.domain.model.Lesson_;
import de.codillas.shared.domain.repository.SortableRepository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID>, SortableRepository<Lesson> {

  /** Lessons in builder/display order. */
  Sort BY_ORDER = Sort.by(Sort.Order.asc(Lesson_.SORT_ORDER));

  List<Lesson> findBySectionId(UUID sectionId, Sort sort);

  List<Lesson> findBySectionIdIn(Collection<UUID> sectionIds, Sort sort);

  void deleteBySectionId(UUID sectionId);
}
