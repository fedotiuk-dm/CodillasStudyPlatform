package de.codillas.homework.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RubricServiceImpl implements RubricService {

  private final AssignmentRepository assignmentRepository;
  private final RubricRepository rubricRepository;
  private final RubricCriterionRepository criterionRepository;
  private final RubricMapper mapper;

  @Override
  @Transactional
  public RubricResponse createRubric(UUID assignmentId, CreateRubricRequest request) {
    Assignment assignment =
        assignmentRepository
            .findById(assignmentId)
            .orElseThrow(() -> new NotFoundException("Assignment", assignmentId));
    // Replace any prior rubric (re-author): drop old criteria + rubric.
    rubricRepository
        .findByAssignmentId(assignmentId)
        .ifPresent(
            old -> {
              criterionRepository.deleteByRubricId(old.getId());
              rubricRepository.delete(old);
            });
    Rubric rubric = rubricRepository.save(Rubric.builder().assignmentId(assignmentId).build());
    int position = 0;
    List<RubricCriterion> criteria = new ArrayList<>();
    for (CreateRubricCriterionRequest c : request.getCriteria()) {
      criteria.add(criterionRepository.save(mapper.toCriterion(c, rubric.getId(), position++)));
    }
    assignment.setRubricId(rubric.getId());
    assignmentRepository.save(assignment);
    return mapper.toResponse(rubric, mapper.toCriterionResponses(criteria));
  }

  @Override
  public RubricResponse getRubric(UUID assignmentId) {
    Rubric rubric =
        rubricRepository
            .findByAssignmentId(assignmentId)
            .orElseThrow(() -> new NotFoundException("Rubric", assignmentId));
    return mapper.toResponse(
        rubric,
        mapper.toCriterionResponses(
            criterionRepository.findByRubricId(
                rubric.getId(), RubricCriterionRepository.BY_POSITION)));
  }
}
