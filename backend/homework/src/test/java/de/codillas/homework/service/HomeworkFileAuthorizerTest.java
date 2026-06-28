package de.codillas.homework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;

import de.codillas.files.domain.model.FileReferenceType;
import de.codillas.shared.security.CurrentUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomeworkFileAuthorizer")
class HomeworkFileAuthorizerTest {

  @Mock private CurrentUser currentUser;

  @Test
  @DisplayName("governs HOMEWORK files")
  void governsHomework() {
    assertThat(new HomeworkFileAuthorizer(currentUser).referenceType())
        .isEqualTo(FileReferenceType.HOMEWORK);
  }

  @Test
  @DisplayName("the uploader may read; a different non-staff user may not; staff may")
  void ownerOrStaff() {
    UUID uploader = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    HomeworkFileAuthorizer authorizer = new HomeworkFileAuthorizer(currentUser);

    when(currentUser.isStaff()).thenReturn(false);
    assertThat(authorizer.canAccess(UUID.randomUUID(), uploader, uploader)).isTrue();
    assertThat(authorizer.canAccess(UUID.randomUUID(), uploader, other)).isFalse();

    when(currentUser.isStaff()).thenReturn(true);
    assertThat(authorizer.canAccess(UUID.randomUUID(), uploader, other)).isTrue();
  }
}
