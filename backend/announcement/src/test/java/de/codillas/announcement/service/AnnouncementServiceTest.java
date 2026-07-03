package de.codillas.announcement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import de.codillas.announcement.api.dto.AnnouncementListResponse;
import de.codillas.announcement.api.dto.AnnouncementResponse;
import de.codillas.announcement.api.dto.CreateAnnouncementRequest;
import de.codillas.announcement.domain.model.Announcement;
import de.codillas.announcement.domain.model.AnnouncementMembership;
import de.codillas.announcement.domain.repository.AnnouncementMembershipRepository;
import de.codillas.announcement.domain.repository.AnnouncementRepository;
import de.codillas.announcement.mapper.AnnouncementMapper;
import de.codillas.shared.event.AnnouncementPosted;
import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.exception.NotFoundException;
import de.codillas.shared.security.CurrentUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementService")
class AnnouncementServiceTest {

  @Mock private AnnouncementRepository repository;
  @Mock private AnnouncementMembershipRepository membershipRepository;
  @Mock private AnnouncementMapper mapper;
  @Mock private CurrentUser currentUser;
  @Mock private ApplicationEventPublisher events;
  @InjectMocks private AnnouncementServiceImpl service;

  @Test
  @DisplayName("a group member reads the group's stream")
  void listGroup_member_ok() {
    UUID groupId = UUID.randomUUID();
    UUID caller = UUID.randomUUID();
    when(currentUser.isStaff()).thenReturn(false);
    when(currentUser.id()).thenReturn(caller);
    when(membershipRepository.existsByGroupIdAndStudentId(groupId, caller)).thenReturn(true);
    Page<Announcement> page = new PageImpl<>(List.of());
    when(repository.findByGroupId(eq(groupId), any(Pageable.class))).thenReturn(page);
    AnnouncementListResponse dto = mock(AnnouncementListResponse.class);
    when(mapper.toListResponse(page)).thenReturn(dto);

    assertThat(service.listGroupAnnouncements(groupId, PageRequest.of(0, 20))).isSameAs(dto);
  }

  @Test
  @DisplayName("a non-member non-staff caller gets 404, never 403")
  void listGroup_nonMember_notFound() {
    UUID groupId = UUID.randomUUID();
    when(currentUser.isStaff()).thenReturn(false);
    when(currentUser.id()).thenReturn(UUID.randomUUID());
    when(membershipRepository.existsByGroupIdAndStudentId(eq(groupId), any())).thenReturn(false);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.listGroupAnnouncements(groupId, PageRequest.of(0, 20)));
  }

  @Test
  @DisplayName("staff reads any group's stream without membership")
  void listGroup_staff_ok() {
    UUID groupId = UUID.randomUUID();
    when(currentUser.isStaff()).thenReturn(true);
    Page<Announcement> page = new PageImpl<>(List.of());
    when(repository.findByGroupId(eq(groupId), any(Pageable.class))).thenReturn(page);
    AnnouncementListResponse dto = mock(AnnouncementListResponse.class);
    when(mapper.toListResponse(page)).thenReturn(dto);

    assertThat(service.listGroupAnnouncements(groupId, PageRequest.of(0, 20))).isSameAs(dto);
  }

  @Test
  @DisplayName("my announcements for a student with no groups is empty (no IN () query)")
  void listMy_noGroups_empty() {
    when(currentUser.isStaff()).thenReturn(false);
    when(currentUser.id()).thenReturn(UUID.randomUUID());
    when(membershipRepository.findByStudentId(any())).thenReturn(List.of());
    AnnouncementListResponse dto = mock(AnnouncementListResponse.class);
    when(mapper.toListResponse(any(Page.class))).thenReturn(dto);

    assertThat(service.listMyAnnouncements(PageRequest.of(0, 20))).isSameAs(dto);
    verify(repository, never()).findByGroupIdIn(any(), any());
  }

  @Test
  @DisplayName("posting saves the announcement and publishes AnnouncementPosted")
  void create_publishesEvent() {
    UUID groupId = UUID.randomUUID();
    UUID author = UUID.randomUUID();
    when(currentUser.id()).thenReturn(author);
    CreateAnnouncementRequest request = mock(CreateAnnouncementRequest.class);
    Announcement saved = Announcement.builder().groupId(groupId).title("Exam moved").build();
    when(mapper.toEntity(groupId, author, request)).thenReturn(saved);
    when(repository.save(saved)).thenReturn(saved);
    AnnouncementResponse dto = mock(AnnouncementResponse.class);
    when(mapper.toResponse(saved)).thenReturn(dto);

    assertThat(service.create(groupId, request)).isSameAs(dto);
    verify(events).publishEvent(new AnnouncementPosted(saved.getId(), groupId, "Exam moved"));
  }

  @Test
  @DisplayName("updating a missing announcement is 404")
  void update_missing_notFound() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service.update(id, null));
  }

  @Test
  @DisplayName("group deletion purges the group's announcements and roster")
  void onGroupDeleted_purges() {
    UUID groupId = UUID.randomUUID();
    service.onGroupDeleted(new GroupDeleted(groupId));
    verify(repository).deleteByGroupId(groupId);
    verify(membershipRepository).deleteByGroupId(groupId);
  }

  @Test
  @DisplayName("re-delivered StudentEnrolled does not duplicate the roster row")
  void onStudentEnrolled_idempotent() {
    UUID groupId = UUID.randomUUID();
    UUID student = UUID.randomUUID();
    when(membershipRepository.existsByGroupIdAndStudentId(groupId, student)).thenReturn(true);

    service.onStudentEnrolled(new StudentEnrolled(groupId, student));

    verify(membershipRepository, never()).save(any(AnnouncementMembership.class));
  }
}
