package de.codillas.enrollment.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.enrollment.api.dto.CreateGroupRequest;
import de.codillas.enrollment.api.dto.GroupListResponse;
import de.codillas.enrollment.api.dto.GroupResponse;
import de.codillas.enrollment.domain.repository.GroupRepository;
import de.codillas.enrollment.mapper.GroupMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupServiceImpl implements GroupService {

  private final GroupRepository repository;
  private final GroupMapper mapper;

  @Override
  @Transactional
  public GroupResponse createGroup(CreateGroupRequest request) {
    return mapper.toResponse(repository.save(mapper.toEntity(request)));
  }

  @Override
  public GroupListResponse listGroups(Pageable pageable) {
    return mapper.toListResponse(repository.findAll(pageable));
  }
}
