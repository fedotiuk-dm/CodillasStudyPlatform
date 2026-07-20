package de.codillas.course.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.course.domain.model.Material;
import de.codillas.course.domain.model.Material_;
import de.codillas.shared.domain.repository.SortableRepository;

@Repository
public interface MaterialRepository
    extends JpaRepository<Material, UUID>, SortableRepository<Material> {

  /** Materials in builder/display order. */
  Sort BY_ORDER = Sort.by(Sort.Order.asc(Material_.SORT_ORDER));

  List<Material> findByLessonId(UUID lessonId, Sort sort);

  void deleteByLessonId(UUID lessonId);

  /** Materials orphaned by a file deleted upstream in files. */
  void deleteByFileId(UUID fileId);

  void deleteByLessonIdIn(Collection<UUID> lessonIds);
}
