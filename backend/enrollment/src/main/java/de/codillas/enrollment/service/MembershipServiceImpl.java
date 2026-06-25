package de.codillas.enrollment.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.enrollment.api.dto.EnrollStudentRequest;
import de.codillas.enrollment.api.dto.MembershipResponse;
import de.codillas.enrollment.domain.model.Membership;
import de.codillas.enrollment.domain.repository.MembershipRepository;
import de.codillas.enrollment.mapper.MembershipMapper;
import de.codillas.shared.event.StudentEnrolled;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipServiceImpl implements MembershipService {

  private final MembershipRepository repository;
  private final MembershipMapper mapper;
  private final ApplicationEventPublisher events;

  @Override
  @Transactional
  public MembershipResponse enrollStudent(UUID groupId, EnrollStudentRequest request) {
    Membership saved = repository.save(mapper.toEntity(request, groupId));
    events.publishEvent(new StudentEnrolled(groupId, request.getUserId()));
    return mapper.toResponse(saved);
  }

  @Override
  public List<MembershipResponse> listGroupMembers(UUID groupId) {
    return mapper.toResponseList(repository.findByGroupId(groupId));
  }
}
