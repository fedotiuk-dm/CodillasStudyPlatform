package de.codillas.assessment.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.assessment.domain.model.Option;
import de.codillas.assessment.domain.model.Option_;

@Repository
public interface OptionRepository extends JpaRepository<Option, UUID> {

  /** Options in display order. */
  Sort BY_POSITION = Sort.by(Sort.Order.asc(Option_.POSITION));

  List<Option> findByQuestionId(UUID questionId, Sort sort);

  List<Option> findByQuestionIdIn(Collection<UUID> questionIds);
}
