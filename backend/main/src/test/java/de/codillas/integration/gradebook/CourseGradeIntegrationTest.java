package de.codillas.integration.gradebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.closeTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import de.codillas.enrollment.domain.model.CourseStatusView;
import de.codillas.enrollment.domain.repository.CourseStatusViewRepository;
import de.codillas.integration.BaseIntegrationTest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Course grade (weighted homework + test, integration)")
class CourseGradeIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CourseStatusViewRepository courseStatus;

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(j -> j.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  /**
   * Create a DRAFT course, publish it, and await enrollment's local read model catching up via the
   * {@code CoursePublished} event — a group can only be created against a PUBLISHED course.
   */
  private UUID createPublishedCourse() throws Exception {
    String course =
        mockMvc
            .perform(
                post("/api/courses")
                    .with(as(UUID.randomUUID(), "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Course %s\"}".formatted(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID courseId = UUID.fromString(JsonPath.read(course, "$.id"));
    mockMvc
        .perform(post("/api/courses/{id}/publish", courseId).with(as(UUID.randomUUID(), "ADMIN")))
        .andExpect(status().isOk());
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                assertThat(courseStatus.findById(courseId))
                    .get()
                    .extracting(CourseStatusView::getStatus)
                    .isEqualTo("PUBLISHED"));
    return courseId;
  }

  @Test
  @DisplayName("final grade weights Σawarded/Σmax across a homework and a test in the group")
  void weightedCourseGrade() throws Exception {
    UUID admin = UUID.randomUUID();
    UUID teacher = UUID.randomUUID();
    UUID student = UUID.randomUUID();

    // a real group is required: group_members has a FK to study_groups
    String group =
        mockMvc
            .perform(
                post("/api/groups")
                    .with(as(admin, "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Cohort\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
                            .formatted(createPublishedCourse(), teacher)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID groupId = UUID.fromString(JsonPath.read(group, "$.id"));

    // enrol the student (creates the gradebook membership; the test attempt's group resolves from
    // it)
    mockMvc
        .perform(
            post("/api/groups/{id}/members", groupId)
                .with(as(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"%s\"}".formatted(student)))
        .andExpect(status().isCreated());

    // homework: 88 / 100
    String assignment =
        mockMvc
            .perform(
                post("/api/assignments")
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"groupId\":\"%s\",\"title\":\"HW\"}".formatted(groupId)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID assignmentId = UUID.fromString(JsonPath.read(assignment, "$.id"));
    String submission =
        mockMvc
            .perform(
                post("/api/assignments/{id}/submissions", assignmentId)
                    .with(as(student, "STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"answer\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID submissionId = UUID.fromString(JsonPath.read(submission, "$.id"));
    mockMvc
        .perform(put("/api/submissions/{id}/submit", submissionId).with(as(student, "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/submissions/{id}/grade", submissionId)
                .with(as(teacher, "TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":88}"))
        .andExpect(status().isCreated());

    // test: one SINGLE_CHOICE question worth 10, answered correctly -> 10 / 10
    String test =
        mockMvc
            .perform(
                post("/api/tests")
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Quiz\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID testId = UUID.fromString(JsonPath.read(test, "$.id"));
    String question =
        mockMvc
            .perform(
                post("/api/tests/{id}/questions", testId)
                    .with(as(teacher, "TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"type\":\"SINGLE_CHOICE\",\"prompt\":\"2+2?\",\"points\":10,"
                            + "\"options\":[{\"text\":\"4\",\"correct\":true},"
                            + "{\"text\":\"5\",\"correct\":false}]}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID optionId = UUID.fromString(JsonPath.read(question, "$.options[0].id"));
    UUID questionId = UUID.fromString(JsonPath.read(question, "$.id"));
    mockMvc
        .perform(post("/api/tests/{id}/publish", testId).with(as(teacher, "TEACHER")))
        .andExpect(status().isOk());
    String attempt =
        mockMvc
            .perform(post("/api/tests/{id}/attempts", testId).with(as(student, "STUDENT")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID attemptId = UUID.fromString(JsonPath.read(attempt, "$.id"));
    mockMvc
        .perform(
            put("/api/attempts/{id}/answers", attemptId)
                .with(as(student, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"questionId\":\"%s\",\"selectedOptionIds\":[\"%s\"]}"
                        .formatted(questionId, optionId)))
        .andExpect(status().isOk());
    mockMvc
        .perform(post("/api/attempts/{id}/submit", attemptId).with(as(student, "STUDENT")))
        .andExpect(status().isOk());

    // both events are delivered asynchronously via the publication registry
    // weighted = (88 + 10) / (100 + 10) = 98 / 110 ~= 89.09%
    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () ->
                mockMvc
                    .perform(
                        get("/api/gradebook/groups/{id}", groupId).with(as(teacher, "TEACHER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.students[0].courseGrade.awarded").value(98))
                    .andExpect(jsonPath("$.students[0].courseGrade.maxPoints").value(110))
                    .andExpect(
                        jsonPath("$.students[0].courseGrade.percent", closeTo(89.0909, 0.001)))
                    .andExpect(jsonPath("$.students[0].courseGrade.byType.length()").value(2)));

    // re-grade the SAME homework submission with a lower score: 50 / 100.
    // This re-publishes SubmissionGraded and must update the single ProgressEntry in place —
    // not append a second one. New weighted = (50 + 10) / (100 + 10) = 60 / 110 ~= 54.5454%.
    mockMvc
        .perform(
            post("/api/submissions/{id}/grade", submissionId)
                .with(as(teacher, "TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":50}"))
        .andExpect(status().isCreated());

    // await the real SubmissionGraded re-delivery: the final course grade recomputes from the
    // single updated entry (NOT double-counted), and the per-type breakdown count stays at 2.
    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () ->
                mockMvc
                    .perform(
                        get("/api/gradebook/groups/{id}", groupId).with(as(teacher, "TEACHER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.students[0].courseGrade.awarded").value(60))
                    .andExpect(jsonPath("$.students[0].courseGrade.maxPoints").value(110))
                    .andExpect(
                        jsonPath("$.students[0].courseGrade.percent", closeTo(54.5454, 0.001)))
                    .andExpect(jsonPath("$.students[0].courseGrade.byType.length()").value(2)));
  }
}
