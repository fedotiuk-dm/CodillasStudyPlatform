package de.codillas.course.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.course.domain.model.Section;
import de.codillas.course.domain.model.Section_;
import de.codillas.shared.domain.repository.SortableRepository;

@Repository
public interface SectionRepository
    extends JpaRepository<Section, UUID>, SortableRepository<Section> {

  /** Sections in builder/display order. */
  Sort BY_ORDER = Sort.by(Sort.Order.asc(Section_.SORT_ORDER));

  List<Section> findByCourseId(UUID courseId, Sort sort);
}
