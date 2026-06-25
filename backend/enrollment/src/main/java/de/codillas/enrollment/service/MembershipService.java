package de.codillas.enrollment.service;

import java.util.List;
import java.util.UUID;

import de.codillas.enrollment.api.dto.EnrollStudentRequest;
import de.codillas.enrollment.api.dto.MembershipResponse;

public interface MembershipService {

  MembershipResponse enrollStudent(UUID groupId, EnrollStudentRequest request);

  List<MembershipResponse> listGroupMembers(UUID groupId);
}
