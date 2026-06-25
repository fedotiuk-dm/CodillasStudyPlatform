package de.codillas.enrollment.domain.repository;

import de.codillas.enrollment.domain.model.Group;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

  List<Group> findByCourseId(UUID courseId);
}
