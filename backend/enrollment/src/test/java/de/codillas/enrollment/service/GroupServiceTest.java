package de.codillas.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import de.codillas.enrollment.api.dto.CreateGroupRequest;
import de.codillas.enrollment.api.dto.GroupListResponse;
import de.codillas.enrollment.api.dto.GroupResponse;
import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.repository.GroupRepository;
import de.codillas.enrollment.mapper.GroupMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupService")
class GroupServiceTest {

  @Mock private GroupRepository repository;
  @Mock private GroupMapper mapper;
  @InjectMocks private GroupServiceImpl service;

  @Test
  @DisplayName("createGroup maps the request, persists it, and returns the response")
  void createGroup_mapsPersistsAndReturns() {
    CreateGroupRequest request =
        new CreateGroupRequest("Cohort A", UUID.randomUUID(), UUID.randomUUID());
    Group toSave = Group.builder().name("Cohort A").build();
    Group saved = Group.builder().name("Cohort A").build();
    GroupResponse response =
        new GroupResponse(UUID.randomUUID(), "Cohort A", UUID.randomUUID(), UUID.randomUUID());

    when(mapper.toEntity(request)).thenReturn(toSave);
    when(repository.save(toSave)).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(response);

    assertThat(service.createGroup(request)).isSameAs(response);
    verify(repository).save(toSave);
  }

  @Test
  @DisplayName("listGroups maps the repository page to the list response")
  void listGroups_returnsMappedPage() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Group> page = new PageImpl<>(List.of(Group.builder().name("Cohort A").build()));
    GroupListResponse expected = mock(GroupListResponse.class);

    when(repository.findAll(pageable)).thenReturn(page);
    when(mapper.toListResponse(page)).thenReturn(expected);

    assertThat(service.listGroups(pageable)).isSameAs(expected);
  }
}
