package de.codillas.gradebook.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.gradebook.domain.model.GradeSource;
import de.codillas.gradebook.domain.model.ProgressEntry;
import de.codillas.shared.domain.BaseAuditableEntity_;

@Repository
public interface ProgressEntryRepository extends JpaRepository<ProgressEntry, UUID> {

  /** Oldest first — chronological journal. */
  Sort BY_RECORDED = Sort.by(Sort.Order.asc(BaseAuditableEntity_.CREATED_AT));

  List<ProgressEntry> findByStudentId(UUID studentId, Sort sort);

  List<ProgressEntry> findByStudentIdInAndGroupId(
      Collection<UUID> studentIds, UUID groupId, Sort sort);

  Optional<ProgressEntry> findBySourceAndSourceId(GradeSource source, UUID sourceId);

  Optional<ProgressEntry> findBySourceAndReferenceIdAndStudentId(
      GradeSource source, UUID referenceId, UUID studentId);
}
