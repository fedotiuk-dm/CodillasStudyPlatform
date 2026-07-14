package de.codillas.enrollment.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import de.codillas.enrollment.api.dto.CreateGroupRequest;
import de.codillas.enrollment.api.dto.GroupListResponse;
import de.codillas.enrollment.api.dto.GroupResponse;
import de.codillas.enrollment.api.dto.GroupStatus;
import de.codillas.shared.event.CourseDeleted;

public interface GroupService {

  GroupResponse createGroup(CreateGroupRequest request);

  GroupListResponse listGroups(GroupStatus status, Pageable pageable);

  GroupResponse startGroup(UUID groupId);

  GroupResponse archiveGroup(UUID groupId);

  void deleteGroup(UUID groupId);

  void onCourseDeleted(CourseDeleted event);
}
