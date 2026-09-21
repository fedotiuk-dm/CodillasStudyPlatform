package de.codillas.integration.files;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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
@DisplayName("Files (S3/minio, integration)")
class FilesIntegrationTest extends BaseIntegrationTest {

  private static final String BUCKET = "codillas";

  @Container
  static final MinIOContainer MINIO =
      new MinIOContainer(
          // Docker Hub no longer serves minio/minio; MinIO publishes to quay.io.
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
      s3Client.createBucket(builder -> builder.bucket(BUCKET));
    } catch (BucketAlreadyOwnedByYouException ignored) {
      // already created by a previous test
    }
  }

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Test
  @DisplayName("upload to minio, download the same bytes, then delete")
  void uploadDownloadDelete() throws Exception {
    UUID student = UUID.randomUUID();
    byte[] bytes = "hello codillas".getBytes();
    MockMultipartFile part = new MockMultipartFile("file", "note.txt", "text/plain", bytes);

    String stored =
        mockMvc
            .perform(multipart("/api/files").file(part).with(as(student, "STUDENT")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.originalFilename").value("note.txt"))
            .andExpect(jsonPath("$.fileSize").value(bytes.length))
            .andExpect(jsonPath("$.uploadedBy").value(student.toString()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID fileId = UUID.fromString(JsonPath.read(stored, "$.id"));

    byte[] downloaded =
        mockMvc
            .perform(get("/api/files/{id}", fileId).with(as(student, "STUDENT")))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        "Content-Disposition", org.hamcrest.Matchers.containsString("note.txt")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    org.assertj.core.api.Assertions.assertThat(downloaded).isEqualTo(bytes);

    mockMvc
        .perform(delete("/api/files/{id}", fileId).with(as(UUID.randomUUID(), "TEACHER")))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/files/{id}/metadata", fileId).with(as(student, "STUDENT")))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("my files lists only the caller's uploads, newest first")
  void listMyFiles_onlyOwnUploads() throws Exception {
    UUID me = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    MockMultipartFile mine =
        new MockMultipartFile("file", "mine.txt", "text/plain", "mine".getBytes());
    MockMultipartFile theirs =
        new MockMultipartFile("file", "theirs.txt", "text/plain", "theirs".getBytes());

    mockMvc
        .perform(multipart("/api/files").file(mine).with(as(me, "STUDENT")))
        .andExpect(status().isCreated());
    mockMvc
        .perform(multipart("/api/files").file(theirs).with(as(other, "STUDENT")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/files/my").with(as(me, "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.originalFilename=='mine.txt')]").exists())
        .andExpect(jsonPath("$.content[?(@.originalFilename=='theirs.txt')]").doesNotExist());
  }

  @Test
  @DisplayName("a student cannot delete files (403)")
  void delete_asStudent_returns403() throws Exception {
    mockMvc
        .perform(
            delete("/api/files/{id}", UUID.randomUUID()).with(as(UUID.randomUUID(), "STUDENT")))
        .andExpect(status().isForbidden());
  }
}
