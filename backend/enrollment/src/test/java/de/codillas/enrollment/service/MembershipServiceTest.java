package de.codillas.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.enrollment.api.dto.EnrollStudentRequest;
import de.codillas.enrollment.api.dto.MembershipResponse;
import de.codillas.enrollment.domain.model.Membership;
import de.codillas.enrollment.domain.repository.MembershipRepository;
import de.codillas.enrollment.mapper.MembershipMapper;
import de.codillas.shared.event.StudentEnrolled;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipService")
class MembershipServiceTest {

  @Mock private MembershipRepository repository;
  @Mock private MembershipMapper mapper;
  @Mock private ApplicationEventPublisher events;
  @InjectMocks private MembershipServiceImpl service;

  @Test
  @DisplayName(
      "enrollStudent saves the membership, publishes StudentEnrolled, and returns the response")
  void enrollStudent_savesPublishesAndReturns() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    EnrollStudentRequest request = new EnrollStudentRequest(userId);
    Membership saved = Membership.builder().groupId(groupId).userId(userId).build();
    MembershipResponse dto = new MembershipResponse(UUID.randomUUID(), groupId, userId);
    when(repository.save(any(Membership.class))).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(dto);

    assertThat(service.enrollStudent(groupId, request)).isSameAs(dto);
    verify(events).publishEvent(new StudentEnrolled(groupId, userId));
  }

  @Test
  @DisplayName("listGroupMembers maps the group's memberships")
  void listGroupMembers_mapsMembers() {
    UUID groupId = UUID.randomUUID();
    List<Membership> members =
        List.of(Membership.builder().groupId(groupId).userId(UUID.randomUUID()).build());
    List<MembershipResponse> dtos = List.of(mock(MembershipResponse.class));
    when(repository.findByGroupId(groupId)).thenReturn(members);
    when(mapper.toResponseList(members)).thenReturn(dtos);

    assertThat(service.listGroupMembers(groupId)).isSameAs(dtos);
  }
}
