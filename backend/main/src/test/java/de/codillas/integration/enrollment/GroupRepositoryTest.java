package de.codillas.integration.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.repository.GroupRepository;
import de.codillas.integration.BaseIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("GroupRepository (integration)")
class GroupRepositoryTest extends BaseIntegrationTest {

  @Autowired private GroupRepository repository;

  @Test
  @DisplayName("findByCourseId returns only the groups created for that course")
  void findByCourseId_returnsGroupsForCourse() {
    UUID courseId = UUID.randomUUID();
    repository.save(group("Cohort A", courseId));
    repository.save(group("Cohort B", courseId));
    repository.save(group("Other course", UUID.randomUUID()));

    var groups = repository.findByCourseId(courseId);

    assertThat(groups)
        .hasSize(2)
        .extracting(Group::getName)
        .containsExactlyInAnyOrder("Cohort A", "Cohort B");
  }

  private static Group group(String name, UUID courseId) {
    return Group.builder().name(name).courseId(courseId).teacherId(UUID.randomUUID()).build();
  }
}
