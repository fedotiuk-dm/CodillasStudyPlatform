package de.codillas.homework.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import de.codillas.homework.api.dto.AssignmentListResponse;
import de.codillas.homework.api.dto.AssignmentResponse;
import de.codillas.homework.api.dto.CreateAssignmentRequest;

public interface AssignmentService {

  AssignmentResponse createAssignment(CreateAssignmentRequest request);

  AssignmentResponse publishAssignment(UUID assignmentId);

  AssignmentListResponse listAssignments(UUID groupId, Pageable pageable);

  void onLessonsDeleted(List<UUID> lessonIds);

  void onGroupArchived(UUID groupId);
}
