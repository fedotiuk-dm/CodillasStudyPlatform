package de.codillas.integration.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.integration.BaseIntegrationTest;
import de.codillas.notification.service.RecipientEmailResolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Email channel (UserEmailChanged -> recipient_email, integration)")
class EmailChannelIntegrationTest extends BaseIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired RecipientEmailResolver recipientEmailResolver;

  @Test
  @DisplayName("a provisioned profile makes the recipient email resolvable")
  void resolvesAfterProvisioning() throws Exception {
    UUID student = UUID.randomUUID();
    mockMvc
        .perform(
            get("/api/users/me")
                .with(
                    jwt()
                        .jwt(
                            j ->
                                j.subject(student.toString())
                                    .claim("name", "Stu")
                                    .claim("email", "stu@codillas.de"))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk());

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> assertThat(recipientEmailResolver.resolve(student)).contains("stu@codillas.de"));
  }
}
