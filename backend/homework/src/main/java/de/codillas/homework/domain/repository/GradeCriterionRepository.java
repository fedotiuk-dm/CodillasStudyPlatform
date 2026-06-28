package de.codillas.homework.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.homework.domain.model.GradeCriterion;

@Repository
public interface GradeCriterionRepository extends JpaRepository<GradeCriterion, UUID> {

  List<GradeCriterion> findByGradeId(UUID gradeId);

  void deleteByGradeId(UUID gradeId);
}
