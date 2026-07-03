package de.codillas.announcement.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.announcement.api.dto.AnnouncementListResponse;
import de.codillas.announcement.api.dto.AnnouncementResponse;
import de.codillas.announcement.api.dto.CreateAnnouncementRequest;
import de.codillas.announcement.api.dto.UpdateAnnouncementRequest;
import de.codillas.announcement.domain.model.Announcement;
import de.codillas.announcement.domain.model.AnnouncementMembership;
import de.codillas.announcement.domain.repository.AnnouncementMembershipRepository;
import de.codillas.announcement.domain.repository.AnnouncementRepository;
import de.codillas.announcement.mapper.AnnouncementMapper;
import de.codillas.shared.domain.repository.GenericSpecification;
import de.codillas.shared.event.AnnouncementPosted;
import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.exception.NotFoundException;
import de.codillas.shared.security.CurrentUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnnouncementServiceImpl implements AnnouncementService {

  private final AnnouncementRepository repository;
  private final AnnouncementMembershipRepository membershipRepository;
  private final AnnouncementMapper mapper;
  private final CurrentUser currentUser;
  private final ApplicationEventPublisher events;

  @Override
  public AnnouncementListResponse listGroupAnnouncements(UUID groupId, Pageable pageable) {
    requireMemberOrStaff(groupId);
    return mapper.toListResponse(
        repository.findByGroupId(
            groupId,
            GenericSpecification.withDefaultSort(pageable, AnnouncementRepository.STREAM_ORDER)));
  }

  @Override
  public AnnouncementListResponse listMyAnnouncements(Pageable pageable) {
    Pageable sorted =
        GenericSpecification.withDefaultSort(pageable, AnnouncementRepository.STREAM_ORDER);
    if (currentUser.isStaff()) {
      return mapper.toListResponse(repository.findAll(sorted));
    }
    List<UUID> groupIds =
        membershipRepository.findByStudentId(currentUser.id()).stream()
            .map(AnnouncementMembership::getGroupId)
            .toList();
    if (groupIds.isEmpty()) {
      return mapper.toListResponse(Page.empty(sorted));
    }
    return mapper.toListResponse(repository.findByGroupIdIn(groupIds, sorted));
  }

  @Override
  @Transactional
  public AnnouncementResponse create(UUID groupId, CreateAnnouncementRequest request) {
    Announcement saved = repository.save(mapper.toEntity(groupId, currentUser.id(), request));
    events.publishEvent(new AnnouncementPosted(saved.getId(), groupId, saved.getTitle()));
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public AnnouncementResponse update(UUID announcementId, UpdateAnnouncementRequest request) {
    Announcement announcement = findByIdOrThrow(announcementId);
    mapper.updateEntity(announcement, request);
    return mapper.toResponse(repository.save(announcement));
  }

  @Override
  @Transactional
  public void delete(UUID announcementId) {
    repository.delete(findByIdOrThrow(announcementId));
  }

  @Override
  @Transactional
  public void onStudentEnrolled(StudentEnrolled event) {
    if (!membershipRepository.existsByGroupIdAndStudentId(event.groupId(), event.userId())) {
      membershipRepository.save(mapper.toMembership(event));
    }
  }

  @Override
  @Transactional
  public void onGroupDeleted(GroupDeleted event) {
    repository.deleteByGroupId(event.groupId());
    membershipRepository.deleteByGroupId(event.groupId());
  }

  /** Reads are member-or-staff; a non-member sees 404 (never leak the group's existence). */
  private void requireMemberOrStaff(UUID groupId) {
    if (currentUser.isStaff()
        || membershipRepository.existsByGroupIdAndStudentId(groupId, currentUser.id())) {
      return;
    }
    throw new NotFoundException("Group", groupId);
  }

  private Announcement findByIdOrThrow(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("Announcement", id));
  }
}
