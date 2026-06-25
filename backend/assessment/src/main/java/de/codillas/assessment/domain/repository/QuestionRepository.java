package de.codillas.assessment.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.assessment.domain.model.Question;
import de.codillas.assessment.domain.model.Question_;
import de.codillas.shared.domain.repository.SortableRepository;

@Repository
public interface QuestionRepository
    extends JpaRepository<Question, UUID>, SortableRepository<Question> {

  /** Questions in builder/display order. */
  Sort BY_ORDER = Sort.by(Sort.Order.asc(Question_.SORT_ORDER));

  List<Question> findByTestId(UUID testId, Sort sort);
}
