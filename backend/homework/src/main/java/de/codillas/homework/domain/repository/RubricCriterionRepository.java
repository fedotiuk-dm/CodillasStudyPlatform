package de.codillas.homework.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.homework.domain.model.RubricCriterion;
import de.codillas.homework.domain.model.RubricCriterion_;

@Repository
public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, UUID> {

  /** Criteria in author-defined order. */
  Sort BY_POSITION = Sort.by(Sort.Order.asc(RubricCriterion_.POSITION));

  List<RubricCriterion> findByRubricId(UUID rubricId, Sort sort);

  void deleteByRubricId(UUID rubricId);
}
