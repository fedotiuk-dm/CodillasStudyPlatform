package de.codillas.enrollment.service;

import org.springframework.data.domain.Pageable;

import de.codillas.enrollment.api.dto.CreateGroupRequest;
import de.codillas.enrollment.api.dto.GroupListResponse;
import de.codillas.enrollment.api.dto.GroupResponse;

public interface GroupService {

  GroupResponse createGroup(CreateGroupRequest request);

  GroupListResponse listGroups(Pageable pageable);
}
