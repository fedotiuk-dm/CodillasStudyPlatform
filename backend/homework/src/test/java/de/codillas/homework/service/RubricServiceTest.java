package de.codillas.homework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.codillas.homework.api.dto.CreateRubricCriterionRequest;
import de.codillas.homework.api.dto.CreateRubricRequest;
import de.codillas.homework.api.dto.RubricResponse;
import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.Rubric;
import de.codillas.homework.domain.model.RubricCriterion;
import de.codillas.homework.domain.repository.AssignmentRepository;
import de.codillas.homework.domain.repository.RubricCriterionRepository;
import de.codillas.homework.domain.repository.RubricRepository;
import de.codillas.homework.mapper.RubricMapper;
import de.codillas.shared.exception.NotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RubricServiceImpl")
class RubricServiceTest {

  @Mock AssignmentRepository assignmentRepository;
  @Mock RubricRepository rubricRepository;
  @Mock RubricCriterionRepository criterionRepository;
  @Mock RubricMapper mapper;
  @InjectMocks RubricServiceImpl service;

  @Test
  @DisplayName(
      "createRubric saves a rubric, persists criteria with ascending positions, links the assignment")
  void createRubricPersistsOrderedCriteria() {
    UUID assignmentId = UUID.randomUUID();
    UUID rubricId = UUID.randomUUID();
    Assignment assignment = Assignment.builder().groupId(UUID.randomUUID()).title("HW").build();
    CreateRubricRequest request = new CreateRubricRequest();
    request.addCriteriaItem(new CreateRubricCriterionRequest().label("Correctness").maxPoints(30));
    request.addCriteriaItem(new CreateRubricCriterionRequest().label("Style").maxPoints(10));

    when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(rubricRepository.findByAssignmentId(assignmentId)).thenReturn(Optional.empty());
    when(rubricRepository.save(any(Rubric.class)))
        .thenAnswer(
            inv -> {
              Rubric r = inv.getArgument(0);
              r.setId(rubricId);
              return r;
            });
    when(mapper.toCriterion(any(), eq(rubricId), anyInt()))
        .thenAnswer(
            inv ->
                RubricCriterion.builder()
                    .rubricId(inv.getArgument(1))
                    .position(inv.getArgument(2))
                    .build());
    when(criterionRepository.save(any(RubricCriterion.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    RubricResponse response = new RubricResponse();
    when(mapper.toResponse(any(Rubric.class), any())).thenReturn(response);

    RubricResponse result = service.createRubric(assignmentId, request);

    assertThat(result).isSameAs(response);
    assertThat(assignment.getRubricId()).isEqualTo(rubricId);

    ArgumentCaptor<Integer> positions = ArgumentCaptor.forClass(Integer.class);
    verify(mapper, org.mockito.Mockito.times(2))
        .toCriterion(any(), eq(rubricId), positions.capture());
    assertThat(positions.getAllValues()).containsExactly(0, 1);
    verify(criterionRepository, org.mockito.Mockito.times(2)).save(any(RubricCriterion.class));
    verify(assignmentRepository).save(assignment);
  }

  @Test
  @DisplayName("createRubric replaces a prior rubric: drops its criteria and the old rubric")
  void createRubricReplacesPrior() {
    UUID assignmentId = UUID.randomUUID();
    UUID oldRubricId = UUID.randomUUID();
    UUID newRubricId = UUID.randomUUID();
    Assignment assignment = Assignment.builder().groupId(UUID.randomUUID()).title("HW").build();
    Rubric old = Rubric.builder().assignmentId(assignmentId).build();
    old.setId(oldRubricId);
    CreateRubricRequest request = new CreateRubricRequest();
    request.addCriteriaItem(new CreateRubricCriterionRequest().label("Only").maxPoints(50));

    when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(rubricRepository.findByAssignmentId(assignmentId)).thenReturn(Optional.of(old));
    when(rubricRepository.save(any(Rubric.class)))
        .thenAnswer(
            inv -> {
              Rubric r = inv.getArgument(0);
              r.setId(newRubricId);
              return r;
            });
    when(mapper.toCriterion(any(), eq(newRubricId), anyInt()))
        .thenReturn(RubricCriterion.builder().rubricId(newRubricId).build());
    when(criterionRepository.save(any(RubricCriterion.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(mapper.toResponse(any(Rubric.class), any())).thenReturn(new RubricResponse());

    service.createRubric(assignmentId, request);

    verify(criterionRepository).deleteByRubricId(oldRubricId);
    verify(rubricRepository).delete(old);
  }

  @Test
  @DisplayName("createRubric on a missing assignment throws NotFoundException")
  void createRubricMissingAssignment() {
    UUID assignmentId = UUID.randomUUID();
    when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createRubric(assignmentId, new CreateRubricRequest()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("getRubric returns the rubric with criteria in position order")
  void getRubricReturnsOrdered() {
    UUID assignmentId = UUID.randomUUID();
    UUID rubricId = UUID.randomUUID();
    Rubric rubric = Rubric.builder().assignmentId(assignmentId).build();
    rubric.setId(rubricId);
    List<RubricCriterion> criteria =
        List.of(RubricCriterion.builder().rubricId(rubricId).position(0).build());
    RubricResponse response = new RubricResponse();

    when(rubricRepository.findByAssignmentId(assignmentId)).thenReturn(Optional.of(rubric));
    when(criterionRepository.findByRubricId(rubricId, RubricCriterionRepository.BY_POSITION))
        .thenReturn(criteria);
    when(mapper.toCriterionResponses(criteria)).thenReturn(List.of());
    when(mapper.toResponse(eq(rubric), any())).thenReturn(response);

    assertThat(service.getRubric(assignmentId)).isSameAs(response);
  }

  @Test
  @DisplayName("getRubric throws NotFoundException when the assignment has no rubric")
  void getRubricAbsent() {
    UUID assignmentId = UUID.randomUUID();
    when(rubricRepository.findByAssignmentId(assignmentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getRubric(assignmentId)).isInstanceOf(NotFoundException.class);
  }
}
