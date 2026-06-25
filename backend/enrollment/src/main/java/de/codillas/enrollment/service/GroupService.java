package de.codillas.enrollment.service;

import de.codillas.enrollment.api.dto.CreateGroupRequest;
import de.codillas.enrollment.api.dto.GroupListResponse;
import de.codillas.enrollment.api.dto.GroupResponse;
import org.springframework.data.domain.Pageable;

public interface GroupService {

  GroupResponse createGroup(CreateGroupRequest request);

  GroupListResponse listGroups(Pageable pageable);
}
