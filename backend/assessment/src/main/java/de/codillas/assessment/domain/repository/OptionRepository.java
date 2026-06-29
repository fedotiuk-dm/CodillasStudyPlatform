package de.codillas.assessment.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.assessment.domain.model.Option;

@Repository
public interface OptionRepository extends JpaRepository<Option, UUID> {

  List<Option> findByQuestionIdIn(Collection<UUID> questionIds);
}
