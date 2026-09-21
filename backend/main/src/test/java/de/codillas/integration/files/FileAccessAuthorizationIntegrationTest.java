package de.codillas.integration.files;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.integration.BaseIntegrationTest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

@Testcontainers
@DisplayName("File access authorization (S3/minio, integration)")
class FileAccessAuthorizationIntegrationTest extends BaseIntegrationTest {

  private static final String BUCKET = "codillas";

  @Container
  static final MinIOContainer MINIO =
      new MinIOContainer(
          DockerImageName.parse("quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z")
              .asCompatibleSubstituteFor("minio/minio"));

  @DynamicPropertySource
  static void minioProperties(DynamicPropertyRegistry registry) {
    registry.add("codillas.files.endpoint", MINIO::getS3URL);
    registry.add("codillas.files.access-key", MINIO::getUserName);
    registry.add("codillas.files.secret-key", MINIO::getPassword);
    registry.add("codillas.files.bucket", () -> BUCKET);
    registry.add("codillas.files.region", () -> "us-east-1");
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private S3Client s3Client;

  @BeforeEach
  void ensureBucket() {
    try {
      s3Client.createBucket(b -> b.bucket(BUCKET));
    } catch (BucketAlreadyOwnedByYouException ignored) {
      // already created
    }
  }

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private UUID upload(UUID uploader, String role, String referenceType, UUID referenceId)
      throws Exception {
    MockMultipartFile part =
        new MockMultipartFile("file", "f.txt", "text/plain", "data".getBytes());
    String stored =
        mockMvc
            .perform(
                multipart("/api/files")
                    .file(part)
                    .param("referenceType", referenceType)
                    .param("referenceId", referenceId.toString())
                    .with(as(uploader, role)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(stored, "$.id"));
  }

  @Test
  @DisplayName(
      "a HOMEWORK file is downloadable by its uploader and staff, but not another student (404)")
  void homeworkFileOwnerOrStaff() throws Exception {
    UUID studentA = UUID.randomUUID();
    UUID studentB = UUID.randomUUID();
    UUID fileId = upload(studentA, "STUDENT", "HOMEWORK", UUID.randomUUID());

    mockMvc
        .perform(get("/api/files/{id}", fileId).with(as(studentA, "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/files/{id}", fileId).with(as(studentB, "STUDENT")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/files/{id}", fileId).with(as(UUID.randomUUID(), "TEACHER")))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("a CHAT file is downloadable by a room member, but not a non-member (404)")
  void chatFileMemberOnly() throws Exception {
    UUID teacher = UUID.randomUUID();
    UUID student = UUID.randomUUID();

    String room =
        mockMvc
            .perform(
                post("/api/chat/rooms")
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"DIRECT\",\"memberIds\":[\"%s\"]}".formatted(student)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID roomId = UUID.fromString(JsonPath.read(room, "$.id"));

    UUID fileId = upload(student, "STUDENT", "CHAT", roomId);

    mockMvc
        .perform(get("/api/files/{id}", fileId).with(as(student, "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/files/{id}", fileId).with(as(UUID.randomUUID(), "STUDENT")))
        .andExpect(status().isNotFound());
  }
}
