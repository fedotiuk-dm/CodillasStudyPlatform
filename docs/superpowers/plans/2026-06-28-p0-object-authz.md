# P0 — Object-level authorization Implementation Plan

> **Status (2026-07-03): IMPLEMENTED** on `feat/lms-hardening` — landed in commit `6af4743`
> (backend P0–P2c) plus the frontend integration commits that follow it. The checkboxes below
> were never ticked during execution; treat this banner, the code, and the git history as the
> source of truth, not the boxes.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the five IDOR / auth-bypass holes the audit found. Today every service gates on *role* but almost never on *ownership*: a student can read/modify another student's submission, attempt, grades, download any file by id, and subscribe to any chat room. Add a caller-vs-resource check at the service layer (404 on mismatch, staff bypass), a `FileAccessAuthorizer` SPI for per-reference-type file access, and a frame-level chat subscription guard.

**Architecture:** A new `CurrentUser.isStaff()` predicate (shared) is the single staff-bypass test. Each module that loads a user-owned aggregate by id replaces its private `findByIdOrThrow(id)` with `findByIdForCaller(id)` (owner-or-staff, else 404 — never leaking existence, mirroring `chat.requireMember`). `gradebook` authorizes its two read endpoints (self-or-staff; group-member-or-staff). `files` gains a `FileAccessAuthorizer` SPI resolved by `FileReferenceType`, implemented in `homework` (owner-or-staff), `chat` (room member) and `files` (MATERIAL → permissive); `download` calls it. `chat` exposes `isMember(roomId,userId)` and `main` registers a `ChatSubscriptionInterceptor` on the client-inbound channel that rejects a non-member SUBSCRIBE to `/topic/chat/{roomId}`.

**Tech Stack:** Spring Modulith (Maven multi-module), Java 25, Spring Security (OAuth2 resource server + STOMP), MapStruct, Liquibase, JUnit 5 + Mockito (module unit tests) + Testcontainers Postgres/MinIO (`main` integration tests). See `backend/AGENTS.md`.

## Global Constraints

- **The `backend/AGENTS.md` chain is binding** and already encodes the HOW (layering, MapStruct, where tests live, ProblemDetail). This plan references it rather than restating it.
- **Decision (from the spec, Batch 1) — ownership mismatch = `NotFoundException` (404), not 403.** Matches `chat.requireMember`; never confirm a resource exists to a caller who may not see it.
- **The check lives in the service layer**, in a private `findByIdForCaller(id)` (or an inline guard for the event-fed gradebook reads). Controllers stay thin and keep their existing `@Requires*` role gates — the object check is *in addition to* the role gate, not a replacement.
- **Staff bypass** = `currentUser.isStaff()` (TEACHER or ADMIN). RoleHierarchy already makes ADMIN ⊇ TEACHER; `isStaff()` tests both raw roles so no hierarchy lookup is needed.
- **TDD.** Unit tests live in the module (`@ExtendWith(MockitoExtension.class)`, no Spring). Integration / negative-authz tests live in `main` (`BaseIntegrationTest`, Testcontainers, mock `jwt()`). **Every IDOR fixed gets a negative-authz integration test.**
- **API-first.** Only `gradebook-paths.yaml` changes (add a `404` response to its two reads); regenerate before wiring. All other affected operations already declare `404`. Generated code is read-only.
- **No new infra dependencies.** This batch *does* add two intra-backend Maven deps (`homework → files`, `chat → files`) so those modules can implement the files SPI — see Task 6/7 and the decision note there.
- Commit after each task (`feat(...)` / `test(...)` / `refactor(...)`); **do not push.** Format with `mvn spotless:apply` before each commit. Bump touched modules' Maven version minor in the final task.
- Canonical names are fixed across batches — use verbatim: `isStaff()`, `hasRole(Role)`, `findByIdForCaller`, `FileAccessAuthorizer`, `ChatSubscriptionInterceptor`, `isMember`.

## File Structure

```
backend/
  shared/.../security/CurrentUser.java                                   MOD  isStaff()+hasRole(Role) default methods
  shared/src/test/.../security/CurrentUserTest.java                      NEW  pure unit test of the predicates

  homework/.../service/SubmissionServiceImpl.java                        MOD  findByIdOrThrow → findByIdForCaller
  homework/src/test/.../service/SubmissionServiceTest.java               MOD  staff stubs + new owner/non-owner/staff cases
  homework/pom.xml                                                       MOD  add dependency on codillas-files (Task 6)
  homework/.../service/HomeworkFileAuthorizer.java                       NEW  HOMEWORK → owner-or-staff
  homework/src/test/.../service/HomeworkFileAuthorizerTest.java          NEW

  assessment/.../service/AttemptServiceImpl.java                         MOD  findByIdOrThrow → findByIdForCaller
  assessment/src/test/.../service/AttemptServiceTest.java                MOD  staff stubs + new owner/non-owner/staff cases

  gradebook/.../service/GradebookServiceImpl.java                        MOD  inject CurrentUser + authorize both reads
  gradebook/src/test/.../service/GradebookServiceTest.java               MOD  CurrentUser mock + staff stub + new cases

  files/.../service/FileAccessAuthorizer.java                            NEW  SPI (keyed by FileReferenceType)
  files/.../service/MaterialFileAuthorizer.java                          NEW  MATERIAL → permissive
  files/.../service/FileServiceImpl.java                                 MOD  download resolves authorizer
  files/src/test/.../service/FileServiceTest.java                        MOD  manual ctor + access cases

  chat/.../service/ChatService.java                                      MOD  boolean isMember(roomId,userId)
  chat/.../service/ChatServiceImpl.java                                  MOD  isMember + requireMember delegates to it
  chat/.../service/ChatFileAuthorizer.java                               NEW  CHAT → room member
  chat/src/test/.../service/ChatServiceTest.java                         MOD  isMember test
  chat/pom.xml                                                           MOD  add dependency on codillas-files (Task 7)
  chat/AGENTS.md                                                         MOD  depends-on: + files

  main/.../config/ChatSubscriptionInterceptor.java                       NEW  SUBSCRIBE-frame membership guard
  main/.../config/WebSocketConfig.java                                   MOD  register the interceptor
  main/src/test/.../config/ChatSubscriptionInterceptorTest.java          NEW  unit (broker is profile-disabled in IT)

  openapi/gradebook-paths.yaml                                           MOD  add 404 to both GET operations

  main/src/test/.../integration/homework/HomeworkAuthorizationIntegrationTest.java     NEW
  main/src/test/.../integration/assessment/AttemptAuthorizationIntegrationTest.java    NEW
  main/src/test/.../integration/gradebook/GradebookAuthorizationIntegrationTest.java   NEW
  main/src/test/.../integration/files/FileAccessAuthorizationIntegrationTest.java      NEW  (MinIO)
```

**Current facts the tasks rely on (verified against the code):**
- `CurrentUser` (shared) exposes `id()`, `displayName()`, `roles(): Set<String>` (raw role names, no `ROLE_` prefix, hierarchy not applied). `Role.name()` is `"ADMIN"`/`"TEACHER"`/`"STUDENT"`.
- `SubmissionServiceImpl` and `AttemptServiceImpl` already inject `CurrentUser`; both aggregates expose `getStudentId()`. Their by-id loads currently go through a private `findByIdOrThrow`.
- `GradebookServiceImpl` does **not** inject `CurrentUser` yet; `GradebookMembershipRepository.existsByGroupIdAndStudentId(groupId, studentId)` already exists.
- `StoredFile.getReferenceType()` returns `de.codillas.files.domain.model.FileReferenceType { HOMEWORK, CHAT, MATERIAL }`; `getReferenceId()`/`getUploadedBy()` are `UUID`. `FileServiceImpl.download(UUID)` currently does no caller check; `FilesController` keeps `@RequiresAuthenticated`.
- `ChatRoomMemberRepository.existsByRoomIdAndUserId(roomId,userId)` exists; `ChatServiceImpl.requireMember` already 404s a non-member.
- `WebSocketConfig` + `StompAuthInterceptor` are `@Profile("!integration-test")` (broker is off under `BaseIntegrationTest`); `configureClientInboundChannel` currently registers only `stompAuthInterceptor`.
- All `main` integration tests use `as(UUID id, String role)` = `jwt().jwt(j -> j.subject(id.toString())).authorities(new SimpleGrantedAuthority("ROLE_" + role))`.

---

### Task 1: `CurrentUser.isStaff()` + `hasRole(Role)` (shared)

**Files:** Modify `backend/shared/src/main/java/de/codillas/shared/security/CurrentUser.java`; create `backend/shared/src/test/java/de/codillas/shared/security/CurrentUserTest.java`.

- [ ] **Step 1 (RED): write the unit test.** Pure JUnit, an inline `CurrentUser` with fixed roles.

  ```java
  package de.codillas.shared.security;

  import static org.assertj.core.api.Assertions.assertThat;

  import java.util.Set;
  import java.util.UUID;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("CurrentUser staff / role predicates")
  class CurrentUserTest {

    private static CurrentUser withRoles(String... roles) {
      return new CurrentUser() {
        @Override public UUID id() { return UUID.randomUUID(); }
        @Override public String displayName() { return "test"; }
        @Override public Set<String> roles() { return Set.of(roles); }
      };
    }

    @Test
    @DisplayName("hasRole matches the granted role name")
    void hasRole() {
      assertThat(withRoles("TEACHER").hasRole(Role.TEACHER)).isTrue();
      assertThat(withRoles("STUDENT").hasRole(Role.TEACHER)).isFalse();
    }

    @Test
    @DisplayName("isStaff is true for TEACHER or ADMIN, false for STUDENT or anonymous")
    void isStaff() {
      assertThat(withRoles("ADMIN").isStaff()).isTrue();
      assertThat(withRoles("TEACHER").isStaff()).isTrue();
      assertThat(withRoles("STUDENT").isStaff()).isFalse();
      assertThat(withRoles().isStaff()).isFalse();
    }
  }
  ```

- [ ] **Step 2 (GREEN): add the default methods** to `CurrentUser` (below `roles()`):

  ```java
    /** True if this request was granted {@code role} (the raw granted role; hierarchy is not applied here). */
    default boolean hasRole(Role role) {
      return roles().contains(role.name());
    }

    /**
     * True for staff — {@link Role#TEACHER} or {@link Role#ADMIN}. The object-level authorization
     * bypass: staff may read and grade any student's work. Tests both raw roles, so no RoleHierarchy
     * lookup is needed.
     */
    default boolean isStaff() {
      return hasRole(Role.ADMIN) || hasRole(Role.TEACHER);
    }
  ```

- [ ] **Step 3: verify + commit.** `cd backend && mvn -pl shared test` (green). `mvn spotless:apply`. `git commit -am "feat(shared): CurrentUser.isStaff() + hasRole(Role) for object-level authz"`.

---

### Task 2: homework — `Submission` ownership (`findByIdForCaller`)

Student owns; review/grade are staff (already `@RequiresTeacher` on the controller, so `isStaff()` passes). Single helper covers both: a student loads only their own; staff loads any.

**Files:** Modify `backend/homework/.../service/SubmissionServiceImpl.java`, `backend/homework/src/test/.../service/SubmissionServiceTest.java`; create `backend/main/src/test/java/de/codillas/integration/homework/HomeworkAuthorizationIntegrationTest.java`.

- [ ] **Step 1 (RED): negative-authz integration test** (`main`).

  ```java
  package de.codillas.integration.homework;

  import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  import java.util.UUID;

  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.http.MediaType;
  import org.springframework.security.core.authority.SimpleGrantedAuthority;
  import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
  import org.springframework.test.web.servlet.MockMvc;

  import de.codillas.integration.BaseIntegrationTest;

  import com.jayway.jsonpath.JsonPath;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("Homework object-level authorization (integration)")
  class HomeworkAuthorizationIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private static JwtRequestPostProcessor as(UUID userId, String role) {
      return jwt().jwt(j -> j.subject(userId.toString()))
          .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    @DisplayName("student B cannot submit student A's submission (404, not 403); A still can")
    void otherStudentCannotTouchSubmission() throws Exception {
      UUID studentA = UUID.randomUUID();
      UUID studentB = UUID.randomUUID();
      UUID assignmentId = UUID.randomUUID();

      String created =
          mockMvc
              .perform(
                  post("/api/assignments/{id}/submissions", assignmentId)
                      .with(as(studentA, "STUDENT"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"content\":\"A's answer\"}"))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();
      UUID submissionId = UUID.fromString(JsonPath.read(created, "$.id"));

      mockMvc
          .perform(put("/api/submissions/{id}/submit", submissionId).with(as(studentB, "STUDENT")))
          .andExpect(status().isNotFound());

      mockMvc
          .perform(put("/api/submissions/{id}/submit", submissionId).with(as(studentA, "STUDENT")))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("staff (teacher) may grade any student's submission")
    void teacherMayGradeAnySubmission() throws Exception {
      UUID studentA = UUID.randomUUID();
      UUID teacher = UUID.randomUUID();
      UUID assignmentId = UUID.randomUUID();

      String created =
          mockMvc
              .perform(
                  post("/api/assignments/{id}/submissions", assignmentId)
                      .with(as(studentA, "STUDENT"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"content\":\"A's answer\"}"))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();
      UUID submissionId = UUID.fromString(JsonPath.read(created, "$.id"));

      mockMvc
          .perform(put("/api/submissions/{id}/submit", submissionId).with(as(studentA, "STUDENT")))
          .andExpect(status().isOk());
      mockMvc
          .perform(
              post("/api/submissions/{id}/grade", submissionId)
                  .with(as(teacher, "TEACHER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"score\":91}"))
          .andExpect(status().isCreated());
    }
  }
  ```

- [ ] **Step 2 (GREEN): replace the helper** in `SubmissionServiceImpl`. Rename `findByIdOrThrow` → `findByIdForCaller` and add the owner-or-staff guard; update the 5 call sites (`updateSubmission`, `submitSubmission`, `reviewSubmission`, `gradeSubmission`, `returnSubmission`):

  ```java
    /**
     * Load a submission the caller is entitled to: its owner (the student) or any staff member.
     * A non-owner non-staff caller is told it does not exist (404, no ownership leak) — mirrors
     * {@code chat.requireMember}.
     */
    private Submission findByIdForCaller(UUID id) {
      Submission submission =
          repository.findById(id).orElseThrow(() -> new NotFoundException("Submission", id));
      if (!currentUser.isStaff() && !submission.getStudentId().equals(currentUser.id())) {
        throw new NotFoundException("Submission", id);
      }
      return submission;
    }
  ```

- [ ] **Step 3: fix + extend the module unit test** (`SubmissionServiceTest`). The five by-id tests now hit the guard, so add a staff stub to each (these ops are exercised as staff here; the owner path is covered by the new cases). To `submitSubmission_transitions`, `updateSubmission_editsDraft`, `returnSubmission_transitions`, `reviewSubmission_transitionsAndSaves`, `gradeSubmission_transitionsSavesAndPublishesEvent` add:

  ```java
      when(currentUser.isStaff()).thenReturn(true);
  ```

  Then add the owner/non-owner/staff cases:

  ```java
    @Test
    @DisplayName("the owner may submit their own submission (non-staff)")
    void submit_owner_allowed() {
      UUID submissionId = UUID.randomUUID();
      UUID owner = UUID.randomUUID();
      Submission submission = Submission.builder().studentId(owner).version(1).build();
      SubmissionResponse dto = mock(SubmissionResponse.class);
      when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(currentUser.isStaff()).thenReturn(false);
      when(currentUser.id()).thenReturn(owner);
      when(repository.save(submission)).thenReturn(submission);
      when(mapper.toResponse(submission)).thenReturn(dto);

      assertThat(service.submitSubmission(submissionId)).isSameAs(dto);
      verify(stateMachine).transitionTo(submission, SubmissionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("a different student gets 404 (not 403) and never transitions the submission")
    void submit_otherStudent_notFound() {
      UUID submissionId = UUID.randomUUID();
      Submission submission = Submission.builder().studentId(UUID.randomUUID()).version(1).build();
      when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(currentUser.isStaff()).thenReturn(false);
      when(currentUser.id()).thenReturn(UUID.randomUUID());

      org.assertj.core.api.Assertions.assertThatExceptionOfType(
              de.codillas.shared.exception.NotFoundException.class)
          .isThrownBy(() -> service.submitSubmission(submissionId));
      verify(stateMachine, org.mockito.Mockito.never())
          .transitionTo(any(), any());
    }
  ```

  (Add `import static org.mockito.ArgumentMatchers.any;` if not present.)

- [ ] **Step 4: verify + commit.** `cd backend && mvn -pl homework test` then `mvn -pl main -am test -Dtest=HomeworkAuthorizationIntegrationTest`. `mvn spotless:apply`. `git commit -am "feat(homework): authorize caller vs submission (404 on mismatch, staff bypass)"`.

---

### Task 3: assessment — `Attempt` ownership (`findByIdForCaller`)

Same shape as Task 2: `saveAnswer`/`submitAttempt`/`getAttempt` → owner; `gradeAnswer` → staff (already `@RequiresTeacher`). `startAttempt` is inherently self-scoped (creates/resumes the caller's own attempt) — leave it unchanged.

**Files:** Modify `backend/assessment/.../service/AttemptServiceImpl.java`, `backend/assessment/src/test/.../service/AttemptServiceTest.java`; create `backend/main/src/test/java/de/codillas/integration/assessment/AttemptAuthorizationIntegrationTest.java`.

- [ ] **Step 1 (RED): negative-authz integration test** (`main`).

  ```java
  package de.codillas.integration.assessment;

  import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  import java.util.UUID;

  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.http.MediaType;
  import org.springframework.security.core.authority.SimpleGrantedAuthority;
  import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
  import org.springframework.test.web.servlet.MockMvc;

  import de.codillas.integration.BaseIntegrationTest;

  import com.jayway.jsonpath.JsonPath;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("Assessment attempt object-level authorization (integration)")
  class AttemptAuthorizationIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private static JwtRequestPostProcessor as(UUID userId, String role) {
      return jwt().jwt(j -> j.subject(userId.toString()))
          .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    @DisplayName("student B cannot read or answer student A's attempt (404); A can read it")
    void otherStudentCannotTouchAttempt() throws Exception {
      UUID teacher = UUID.randomUUID();
      UUID studentA = UUID.randomUUID();
      UUID studentB = UUID.randomUUID();

      String test =
          mockMvc
              .perform(
                  post("/api/tests")
                      .with(as(teacher, "TEACHER"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"title\":\"T\"}"))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();
      UUID testId = UUID.fromString(JsonPath.read(test, "$.id"));

      mockMvc
          .perform(post("/api/tests/{id}/publish", testId).with(as(teacher, "TEACHER")))
          .andExpect(status().isOk());

      String attempt =
          mockMvc
              .perform(post("/api/tests/{id}/attempts", testId).with(as(studentA, "STUDENT")))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();
      UUID attemptId = UUID.fromString(JsonPath.read(attempt, "$.id"));

      mockMvc
          .perform(get("/api/attempts/{id}", attemptId).with(as(studentB, "STUDENT")))
          .andExpect(status().isNotFound());
      mockMvc
          .perform(
              put("/api/attempts/{id}/answers", attemptId)
                  .with(as(studentB, "STUDENT"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"questionId\":\"%s\"}".formatted(UUID.randomUUID())))
          .andExpect(status().isNotFound());

      mockMvc
          .perform(get("/api/attempts/{id}", attemptId).with(as(studentA, "STUDENT")))
          .andExpect(status().isOk());
    }
  }
  ```

- [ ] **Step 2 (GREEN): replace the helper** in `AttemptServiceImpl` (rename `findByIdOrThrow` → `findByIdForCaller`, update the 3 call sites `saveAnswer`/`submitAttempt`/`getAttempt`; `gradeAnswer` also uses it — staff passes):

  ```java
    /** Load an attempt the caller owns, or any attempt for staff. Otherwise 404 (no leak). */
    private Attempt findByIdForCaller(UUID id) {
      Attempt attempt =
          repository.findById(id).orElseThrow(() -> new NotFoundException("Attempt", id));
      if (!currentUser.isStaff() && !attempt.getStudentId().equals(currentUser.id())) {
        throw new NotFoundException("Attempt", id);
      }
      return attempt;
    }
  ```

- [ ] **Step 3: fix + extend the module unit test** (`AttemptServiceTest`). Both `submitAttempt_*` tests build attempts owned by a random student and don't stub `currentUser`, so add the staff stub to each:

  ```java
      when(currentUser.isStaff()).thenReturn(true);
  ```

  Then add owner/non-owner cases for `getAttempt`:

  ```java
    @Test
    @DisplayName("the owner may read their own attempt")
    void getAttempt_owner_allowed() {
      UUID attemptId = UUID.randomUUID();
      UUID owner = UUID.randomUUID();
      Attempt attempt = Attempt.builder().id(attemptId).studentId(owner).build();
      AttemptResponse dto = mock(AttemptResponse.class);
      when(repository.findById(attemptId)).thenReturn(Optional.of(attempt));
      when(currentUser.isStaff()).thenReturn(false);
      when(currentUser.id()).thenReturn(owner);
      when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of());
      when(mapper.toResponse(eq(attempt), any())).thenReturn(dto);

      assertThat(service.getAttempt(attemptId)).isSameAs(dto);
    }

    @Test
    @DisplayName("another student reading the attempt gets 404")
    void getAttempt_otherStudent_notFound() {
      UUID attemptId = UUID.randomUUID();
      Attempt attempt = Attempt.builder().id(attemptId).studentId(UUID.randomUUID()).build();
      when(repository.findById(attemptId)).thenReturn(Optional.of(attempt));
      when(currentUser.isStaff()).thenReturn(false);
      when(currentUser.id()).thenReturn(UUID.randomUUID());

      assertThatExceptionOfType(de.codillas.shared.exception.NotFoundException.class)
          .isThrownBy(() -> service.getAttempt(attemptId));
    }
  ```

- [ ] **Step 4: verify + commit.** `mvn -pl assessment test` then `mvn -pl main -am test -Dtest=AttemptAuthorizationIntegrationTest`. `mvn spotless:apply`. `git commit -am "feat(assessment): authorize caller vs attempt (404 on mismatch, staff bypass)"`.

---

### Task 4: gradebook — authorize the two reads

`getStudentGradebook(studentId)` → self-or-staff; `getGroupGradebook(groupId)` → staff or a member of the local `gradebook_membership` read model. Mismatch = 404.

**Files:** Modify `backend/openapi/gradebook-paths.yaml`, `backend/gradebook/.../service/GradebookServiceImpl.java`, `backend/gradebook/src/test/.../service/GradebookServiceTest.java`; create `backend/main/src/test/java/de/codillas/integration/gradebook/GradebookAuthorizationIntegrationTest.java`.

- [ ] **Step 1: spec + regenerate.** Add a `404` response to **both** GET operations in `gradebook-paths.yaml` (under each `responses:`, after the existing entries), and bump `info.version` `0.1.0` → `0.2.0`:

  ```yaml
        "404":
          $ref: "common.yaml#/components/responses/NotFound"
  ```

  Regenerate: `cd backend && mvn -pl gradebook generate-sources`.

- [ ] **Step 2 (RED): negative-authz integration test** (`main`). Reads are empty-but-200 for the entitled caller (no rows needed), 404 for the unauthorized one.

  ```java
  package de.codillas.integration.gradebook;

  import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  import java.util.UUID;

  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.security.core.authority.SimpleGrantedAuthority;
  import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
  import org.springframework.test.web.servlet.MockMvc;

  import de.codillas.integration.BaseIntegrationTest;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("Gradebook object-level authorization (integration)")
  class GradebookAuthorizationIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private static JwtRequestPostProcessor as(UUID userId, String role) {
      return jwt().jwt(j -> j.subject(userId.toString()))
          .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    @DisplayName("a student may read only their own gradebook; another's is 404; staff reads any")
    void studentGradebookIsSelfOrStaff() throws Exception {
      UUID studentA = UUID.randomUUID();
      UUID studentB = UUID.randomUUID();

      mockMvc
          .perform(get("/api/gradebook/students/{id}", studentA).with(as(studentA, "STUDENT")))
          .andExpect(status().isOk());
      mockMvc
          .perform(get("/api/gradebook/students/{id}", studentA).with(as(studentB, "STUDENT")))
          .andExpect(status().isNotFound());
      mockMvc
          .perform(
              get("/api/gradebook/students/{id}", studentA).with(as(UUID.randomUUID(), "TEACHER")))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a non-member cannot read a group gradebook (404); staff can")
    void groupGradebookIsMemberOrStaff() throws Exception {
      UUID groupId = UUID.randomUUID();

      mockMvc
          .perform(get("/api/gradebook/groups/{id}", groupId).with(as(UUID.randomUUID(), "STUDENT")))
          .andExpect(status().isNotFound());
      mockMvc
          .perform(get("/api/gradebook/groups/{id}", groupId).with(as(UUID.randomUUID(), "TEACHER")))
          .andExpect(status().isOk());
    }
  }
  ```

- [ ] **Step 3 (GREEN): inject `CurrentUser` and guard** in `GradebookServiceImpl`. Add the field (constructor is `@RequiredArgsConstructor`) and the two guards:

  ```java
    private final CurrentUser currentUser; // add after membershipRepository

    @Override
    public StudentGradebookResponse getStudentGradebook(UUID studentId) {
      if (!currentUser.isStaff() && !studentId.equals(currentUser.id())) {
        throw new NotFoundException("Gradebook", studentId);
      }
      return mapper.toStudentGradebook(
          studentId,
          mapper.toEntryResponses(
              repository.findByStudentId(studentId, ProgressEntryRepository.BY_RECORDED)));
    }

    @Override
    public GroupGradebookResponse getGroupGradebook(UUID groupId) {
      if (!currentUser.isStaff()
          && !membershipRepository.existsByGroupIdAndStudentId(groupId, currentUser.id())) {
        throw new NotFoundException("Gradebook", groupId);
      }
      // ...unchanged body...
    }
  ```

  Add imports `de.codillas.shared.exception.NotFoundException` and `de.codillas.shared.security.CurrentUser`.

- [ ] **Step 4: fix + extend the module unit test** (`GradebookServiceTest`). Add `@Mock private CurrentUser currentUser;`. The existing `getStudentGradebook_maps` now hits the guard — add `when(currentUser.isStaff()).thenReturn(true);` to it. Then:

  ```java
    @Test
    @DisplayName("a student reading another student's gradebook gets 404")
    void getStudentGradebook_otherStudent_notFound() {
      UUID owner = UUID.randomUUID();
      when(currentUser.isStaff()).thenReturn(false);
      when(currentUser.id()).thenReturn(UUID.randomUUID());

      org.assertj.core.api.Assertions.assertThatExceptionOfType(
              de.codillas.shared.exception.NotFoundException.class)
          .isThrownBy(() -> service.getStudentGradebook(owner));
    }

    @Test
    @DisplayName("a non-member reading a group gradebook gets 404")
    void getGroupGradebook_nonMember_notFound() {
      UUID groupId = UUID.randomUUID();
      UUID caller = UUID.randomUUID();
      when(currentUser.isStaff()).thenReturn(false);
      when(currentUser.id()).thenReturn(caller);
      when(membershipRepository.existsByGroupIdAndStudentId(groupId, caller)).thenReturn(false);

      org.assertj.core.api.Assertions.assertThatExceptionOfType(
              de.codillas.shared.exception.NotFoundException.class)
          .isThrownBy(() -> service.getGroupGradebook(groupId));
    }
  ```

  (Import `de.codillas.shared.security.CurrentUser`.)

- [ ] **Step 5: verify + commit.** `mvn -pl gradebook test` then `mvn -pl main -am test -Dtest=GradebookAuthorizationIntegrationTest`. `mvn spotless:apply`. `git commit -am "feat(gradebook): authorize student/group reads (self-or-staff, member-or-staff)"`.

---

### Task 5: files — `FileAccessAuthorizer` SPI + MATERIAL authorizer + `download` wiring

**Decision (refines the spec signature — see final report):** the SPI takes **ids**, not the `StoredFile` entity, so an implementing module never imports a files persistence type (the root invariant: never import another module's `@Entity`). It is keyed by the files-owned `FileReferenceType` enum.

**Files:** Create `FileAccessAuthorizer.java`, `MaterialFileAuthorizer.java` under `backend/files/.../service/`; modify `FileServiceImpl.java`, `FileServiceTest.java`.

- [ ] **Step 1: the SPI.** `backend/files/src/main/java/de/codillas/files/service/FileAccessAuthorizer.java`:

  ```java
  package de.codillas.files.service;

  import java.util.UUID;

  import de.codillas.files.domain.model.FileReferenceType;

  /**
   * SPI: the owning module decides whether a caller may read a stored file of a given reference
   * type. Resolved by {@link #referenceType()} at download time and implemented once per module that
   * owns files (homework, chat, files/materials). Receives only ids — never the {@code StoredFile}
   * entity — so an implementing module imports no files persistence type.
   */
  public interface FileAccessAuthorizer {

    /** The reference type this authorizer governs. */
    FileReferenceType referenceType();

    /**
     * @param referenceId what the file is attached to (e.g. a submission id, a chat room id), may be null
     * @param uploaderId the user who uploaded the file
     * @param userId the calling user
     * @return true if the caller may read the file
     */
    boolean canAccess(UUID referenceId, UUID uploaderId, UUID userId);
  }
  ```

- [ ] **Step 2: the MATERIAL authorizer** (permissive — course materials are not per-user secret). `MaterialFileAuthorizer.java`:

  ```java
  package de.codillas.files.service;

  import java.util.UUID;

  import org.springframework.stereotype.Component;

  import de.codillas.files.domain.model.FileReferenceType;

  /** Course materials are visible to any authenticated caller (download is already @RequiresAuthenticated). */
  @Component
  public class MaterialFileAuthorizer implements FileAccessAuthorizer {

    @Override
    public FileReferenceType referenceType() {
      return FileReferenceType.MATERIAL;
    }

    @Override
    public boolean canAccess(UUID referenceId, UUID uploaderId, UUID userId) {
      return true;
    }
  }
  ```

- [ ] **Step 3 (GREEN): wire `download`** in `FileServiceImpl`. Inject `CurrentUser` and `List<FileAccessAuthorizer>`; resolve by the file's reference type, default (no/loose reference type) to uploader-or-staff:

  ```java
    private final CurrentUser currentUser;
    private final List<FileAccessAuthorizer> authorizers;

    @Override
    public FileContent download(UUID fileId) {
      StoredFile file = findByIdOrThrow(fileId);
      if (!canAccess(file)) {
        throw new NotFoundException("File", fileId); // 404, never leak existence
      }
      return new FileContent(
          new ByteArrayResource(storage.get(file.getStorageKey())),
          file.getOriginalFilename(),
          file.getContentType());
    }

    /**
     * Object-level read check, delegated to the owning module via the {@link FileAccessAuthorizer}
     * resolved by the file's reference type. A file with no matching authorizer (loose / untyped
     * upload) is readable only by its uploader or staff.
     */
    private boolean canAccess(StoredFile file) {
      UUID caller = currentUser.id();
      return authorizers.stream()
          .filter(a -> a.referenceType() == file.getReferenceType())
          .findFirst()
          .map(a -> a.canAccess(file.getReferenceId(), file.getUploadedBy(), caller))
          .orElseGet(() -> caller.equals(file.getUploadedBy()) || currentUser.isStaff());
    }
  ```

  Add imports `java.util.List`, `de.codillas.shared.security.CurrentUser`.

- [ ] **Step 4: fix + extend the module unit test** (`FileServiceTest`). `FileServiceImpl` now has two extra ctor args, so drop `@InjectMocks` and build it in `@BeforeEach` with an empty authorizer list; add a `CurrentUser` mock. Replace the field/setup block:

  ```java
    @Mock private StoredFileRepository repository;
    @Mock private ObjectStorage storage;
    @Mock private StoredFileMapper mapper;
    @Mock private CurrentUser currentUser;
    private FileServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
      service = new FileServiceImpl(repository, storage, mapper, currentUser, java.util.List.of());
    }
  ```

  Make the existing `download_returnsContent` pass the new check by owning the file: build it with `.uploadedBy(uploader)` and stub `when(currentUser.id()).thenReturn(uploader);` (with a `UUID uploader = UUID.randomUUID();`). Then add:

  ```java
    @Test
    @DisplayName("download of a loose file by a non-owner non-staff caller is 404")
    void download_loose_nonOwner_notFound() {
      UUID fileId = UUID.randomUUID();
      StoredFile file = StoredFile.builder().storageKey("k").uploadedBy(UUID.randomUUID()).build();
      when(repository.findById(fileId)).thenReturn(Optional.of(file));
      when(currentUser.id()).thenReturn(UUID.randomUUID());
      when(currentUser.isStaff()).thenReturn(false);

      org.assertj.core.api.Assertions.assertThatExceptionOfType(NotFoundException.class)
          .isThrownBy(() -> service.download(fileId));
    }
  ```

  (Add `import de.codillas.shared.security.CurrentUser;`.)

- [ ] **Step 5: verify + commit.** `mvn -pl files test`. `mvn spotless:apply`. `git commit -am "feat(files): FileAccessAuthorizer SPI + caller check on download; MATERIAL permissive"`.

---

### Task 6: homework — `HomeworkFileAuthorizer` (HOMEWORK → owner-or-staff)

A homework attachment's owner is its uploader (the student who attached it). This is the first module→module compile dependency; it realizes the `homework → files` link the homework contract already declares.

**Files:** Modify `backend/homework/pom.xml`; create `HomeworkFileAuthorizer.java` + its unit test under `backend/homework/.../service/`.

- [ ] **Step 1: add the Maven dependency** to `homework/pom.xml` (next to the existing `codillas-shared` dependency):

  ```xml
      <dependency>
        <groupId>de.codillas</groupId>
        <artifactId>codillas-files</artifactId>
        <version>${project.version}</version>
      </dependency>
  ```

- [ ] **Step 2 (RED): unit test.** `HomeworkFileAuthorizerTest.java`:

  ```java
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
  ```

- [ ] **Step 3 (GREEN):** `HomeworkFileAuthorizer.java`:

  ```java
  package de.codillas.homework.service;

  import java.util.UUID;

  import org.springframework.stereotype.Component;

  import de.codillas.files.domain.model.FileReferenceType;
  import de.codillas.files.service.FileAccessAuthorizer;
  import de.codillas.shared.security.CurrentUser;

  import lombok.RequiredArgsConstructor;

  /** A homework attachment is readable by the student who uploaded it (its owner) or any staff. */
  @Component
  @RequiredArgsConstructor
  public class HomeworkFileAuthorizer implements FileAccessAuthorizer {

    private final CurrentUser currentUser;

    @Override
    public FileReferenceType referenceType() {
      return FileReferenceType.HOMEWORK;
    }

    @Override
    public boolean canAccess(UUID referenceId, UUID uploaderId, UUID userId) {
      return userId.equals(uploaderId) || currentUser.isStaff();
    }
  }
  ```

- [ ] **Step 4: verify + commit.** `mvn -pl homework -am test`. `mvn spotless:apply`. `git commit -am "feat(homework): HomeworkFileAuthorizer (HOMEWORK files owner-or-staff)"`.

---

### Task 7: chat — `isMember` + `ChatFileAuthorizer` (CHAT → room member)

**Files:** Modify `backend/chat/pom.xml`, `ChatService.java`, `ChatServiceImpl.java`, `ChatServiceTest.java`, `chat/AGENTS.md`; create `ChatFileAuthorizer.java`; create `backend/main/src/test/java/de/codillas/integration/files/FileAccessAuthorizationIntegrationTest.java`.

- [ ] **Step 1: add the Maven dependency** to `chat/pom.xml` (same block as Task 6, `codillas-files`).

- [ ] **Step 2 (GREEN): expose `isMember`.** In `ChatService` add `boolean isMember(UUID roomId, UUID userId);`. In `ChatServiceImpl` implement it and route `requireMember` through it (behavior unchanged):

  ```java
    @Override
    public boolean isMember(UUID roomId, UUID userId) {
      return memberRepository.existsByRoomIdAndUserId(roomId, userId);
    }

    /** Membership is the access control — non-members are told the room does not exist. */
    private void requireMember(UUID roomId, UUID userId) {
      if (!isMember(roomId, userId)) {
        throw new NotFoundException("Room", roomId);
      }
    }
  ```

- [ ] **Step 3 (GREEN): the CHAT authorizer.** `ChatFileAuthorizer.java` (referenceId is the room id):

  ```java
  package de.codillas.chat.service;

  import java.util.UUID;

  import org.springframework.stereotype.Component;

  import de.codillas.files.domain.model.FileReferenceType;
  import de.codillas.files.service.FileAccessAuthorizer;

  import lombok.RequiredArgsConstructor;

  /** A chat attachment is readable only by a member of the room it was posted to. */
  @Component
  @RequiredArgsConstructor
  public class ChatFileAuthorizer implements FileAccessAuthorizer {

    private final ChatService chatService;

    @Override
    public FileReferenceType referenceType() {
      return FileReferenceType.CHAT;
    }

    @Override
    public boolean canAccess(UUID referenceId, UUID uploaderId, UUID userId) {
      return chatService.isMember(referenceId, userId);
    }
  }
  ```

- [ ] **Step 4: unit test for `isMember`** — add to `ChatServiceTest` (reuses the existing `memberRepository` mock):

  ```java
    @Test
    @DisplayName("isMember reflects the membership row")
    void isMember_reflectsMembership() {
      UUID roomId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      when(memberRepository.existsByRoomIdAndUserId(roomId, userId)).thenReturn(true);
      assertThat(service.isMember(roomId, userId)).isTrue();
    }
  ```

- [ ] **Step 5 (RED→GREEN): the file-access negative integration test** (`main`, needs MinIO — mirror `FilesIntegrationTest`'s container + bucket setup). Covers a HOMEWORK file (owner-or-staff) and a CHAT file (member-only):

  ```java
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
  import software.amazon.awssdk.services.s3.S3Client;
  import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

  @Testcontainers
  @DisplayName("File access authorization (S3/minio, integration)")
  class FileAccessAuthorizationIntegrationTest extends BaseIntegrationTest {

    private static final String BUCKET = "codillas";

    @Container static final MinIOContainer MINIO = new MinIOContainer("minio/minio:latest");

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
      return jwt().jwt(j -> j.subject(userId.toString()))
          .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private UUID upload(UUID uploader, String role, String referenceType, UUID referenceId)
        throws Exception {
      MockMultipartFile part = new MockMultipartFile("file", "f.txt", "text/plain", "data".getBytes());
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
    @DisplayName("a HOMEWORK file is downloadable by its uploader and staff, but not another student (404)")
    void homeworkFileOwnerOrStaff() throws Exception {
      UUID studentA = UUID.randomUUID();
      UUID studentB = UUID.randomUUID();
      UUID fileId = upload(studentA, "STUDENT", "HOMEWORK", UUID.randomUUID());

      mockMvc.perform(get("/api/files/{id}", fileId).with(as(studentA, "STUDENT")))
          .andExpect(status().isOk());
      mockMvc.perform(get("/api/files/{id}", fileId).with(as(studentB, "STUDENT")))
          .andExpect(status().isNotFound());
      mockMvc.perform(get("/api/files/{id}", fileId).with(as(UUID.randomUUID(), "TEACHER")))
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

      mockMvc.perform(get("/api/files/{id}", fileId).with(as(student, "STUDENT")))
          .andExpect(status().isOk());
      mockMvc.perform(get("/api/files/{id}", fileId).with(as(UUID.randomUUID(), "STUDENT")))
          .andExpect(status().isNotFound());
    }
  }
  ```

- [ ] **Step 6: update `chat/AGENTS.md`** — add `files (FileAccessAuthorizer SPI)` to the "Depends on" line (a real new dependency, so the contract changes).

- [ ] **Step 7: verify + commit.** `mvn -pl chat -am test` then `mvn -pl main -am test -Dtest=FileAccessAuthorizationIntegrationTest`. `mvn spotless:apply`. `git commit -am "feat(chat): isMember + ChatFileAuthorizer; CHAT files are member-only"`.

---

### Task 8: main — `ChatSubscriptionInterceptor` (reject non-member SUBSCRIBE)

**Decision (broker is profile-disabled under `integration-test`):** the membership check is verified by a focused **unit test of the interceptor**, not a full STOMP integration test — `WebSocketConfig`/`StompAuthInterceptor` are `@Profile("!integration-test")`, so no live broker exists under `BaseIntegrationTest`. The new interceptor carries the same profile.

**Files:** Create `ChatSubscriptionInterceptor.java` + `ChatSubscriptionInterceptorTest.java`; modify `WebSocketConfig.java`.

- [ ] **Step 1 (RED): unit test.** `backend/main/src/test/java/de/codillas/config/ChatSubscriptionInterceptorTest.java`:

  ```java
  package de.codillas.config;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
  import static org.mockito.Mockito.mock;
  import static org.mockito.Mockito.when;

  import java.util.UUID;

  import org.springframework.messaging.Message;
  import org.springframework.messaging.MessageChannel;
  import org.springframework.messaging.simp.stomp.StompCommand;
  import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
  import org.springframework.messaging.support.MessageBuilder;
  import org.springframework.security.access.AccessDeniedException;

  import de.codillas.chat.service.ChatService;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;

  @ExtendWith(MockitoExtension.class)
  @DisplayName("ChatSubscriptionInterceptor")
  class ChatSubscriptionInterceptorTest {

    @Mock private ChatService chatService;

    private static Message<?> subscribe(UUID roomId, UUID userId) {
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor.setDestination("/topic/chat/" + roomId);
      accessor.setUser(userId::toString); // Principal::getName
      accessor.setLeaveMutable(true);
      return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("a non-member SUBSCRIBE to a room topic is rejected")
    void nonMemberRejected() {
      UUID roomId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      when(chatService.isMember(roomId, userId)).thenReturn(false);
      ChatSubscriptionInterceptor interceptor = new ChatSubscriptionInterceptor(chatService);

      assertThatExceptionOfType(AccessDeniedException.class)
          .isThrownBy(() -> interceptor.preSend(subscribe(roomId, userId), mock(MessageChannel.class)));
    }

    @Test
    @DisplayName("a member SUBSCRIBE passes through unchanged")
    void memberAllowed() {
      UUID roomId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      when(chatService.isMember(roomId, userId)).thenReturn(true);
      ChatSubscriptionInterceptor interceptor = new ChatSubscriptionInterceptor(chatService);

      Message<?> message = subscribe(roomId, userId);
      assertThat(interceptor.preSend(message, mock(MessageChannel.class))).isSameAs(message);
    }
  }
  ```

- [ ] **Step 2 (GREEN): the interceptor.** `backend/main/src/main/java/de/codillas/config/ChatSubscriptionInterceptor.java`:

  ```java
  package de.codillas.config;

  import java.security.Principal;
  import java.util.UUID;

  import org.springframework.context.annotation.Profile;
  import org.springframework.messaging.Message;
  import org.springframework.messaging.MessageChannel;
  import org.springframework.messaging.simp.stomp.StompCommand;
  import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
  import org.springframework.messaging.support.ChannelInterceptor;
  import org.springframework.messaging.support.MessageHeaderAccessor;
  import org.springframework.security.access.AccessDeniedException;
  import org.springframework.stereotype.Component;

  import de.codillas.chat.service.ChatService;

  import lombok.RequiredArgsConstructor;

  import org.jspecify.annotations.NonNull;

  /**
   * Frame-level chat authorization: a STOMP SUBSCRIBE to {@code /topic/chat/{roomId}} is allowed
   * only for a member of that room. Membership is the access control (mirrors
   * {@code chat.requireMember}); a non-member is denied at the channel before any broker subscription
   * is created. Runs after {@link StompAuthInterceptor} has set the user on CONNECT.
   */
  @Component
  @Profile("!integration-test")
  @RequiredArgsConstructor
  public class ChatSubscriptionInterceptor implements ChannelInterceptor {

    private static final String CHAT_TOPIC_PREFIX = "/topic/chat/";

    private final ChatService chatService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
      StompHeaderAccessor accessor =
          MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
      if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
        return message;
      }
      String destination = accessor.getDestination();
      if (destination == null || !destination.startsWith(CHAT_TOPIC_PREFIX)) {
        return message; // other topics are covered by @EnableWebSocketSecurity's authenticated rule
      }
      Principal user = accessor.getUser();
      UUID roomId = parseRoomId(destination);
      if (user == null || roomId == null || !chatService.isMember(roomId, UUID.fromString(user.getName()))) {
        throw new AccessDeniedException("Not a member of chat room");
      }
      return message;
    }

    private static UUID parseRoomId(String destination) {
      try {
        return UUID.fromString(destination.substring(CHAT_TOPIC_PREFIX.length()));
      } catch (IllegalArgumentException _) {
        return null;
      }
    }
  }
  ```

- [ ] **Step 3: register it** in `WebSocketConfig`. Add the field and chain it after `stompAuthInterceptor` (CONNECT auth must run first):

  ```java
    private final StompAuthInterceptor stompAuthInterceptor;
    private final ChatSubscriptionInterceptor chatSubscriptionInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
      registration.interceptors(stompAuthInterceptor, chatSubscriptionInterceptor);
    }
  ```

- [ ] **Step 4: verify + commit.** `mvn -pl main -am test -Dtest=ChatSubscriptionInterceptorTest`. `mvn spotless:apply`. `git commit -am "feat(main): ChatSubscriptionInterceptor rejects non-member SUBSCRIBE to /topic/chat/{roomId}"`.

---

### Task 9: versioning, docs, full verify

- [ ] **Step 1: bump versions.** Per `backend/AGENTS.md` (minor bump per batch), raise the Maven `version` of every touched module — `shared`, `homework`, `assessment`, `gradebook`, `files`, `chat`, `main` — by one minor (`0.x.y-SNAPSHOT` → `0.(x+1).0-SNAPSHOT`). The `gradebook` OpenAPI `info.version` was already bumped in Task 4.
- [ ] **Step 2: document the new convention.** In `backend/AGENTS.md` (Security section), add the cross-cutting rule the spec calls for: *"Service methods loading a user-owned aggregate authorize the caller against the resource via a private `findByIdForCaller` (404 on mismatch, `isStaff()` bypass), not only the role."* In `files/AGENTS.md`, note the `FileAccessAuthorizer` SPI (implemented per owning module, resolved by `FileReferenceType`).
- [ ] **Step 3: full build + format gate.** `cd backend && mvn -q compile && mvn spotless:check && mvn test` (Testcontainers Postgres + MinIO must be available). Expected: green, including all four new `*AuthorizationIntegrationTest` classes and the interceptor unit test.
- [ ] **Step 4: commit.** `git commit -am "chore: version bump + authorize-the-caller convention (P0 object-authz)"`.

---

## Self-Review

**Spec coverage (Batch 1):**
- `CurrentUser.isStaff()` + `hasRole(Role)` → Task 1. ✓
- homework `findByIdForCaller` (student owns; review/grade staff; 404 on mismatch) → Task 2. ✓
- assessment `findByIdForCaller` (saveAnswer/submit/getAttempt owner; gradeAnswer staff) → Task 3. ✓
- gradebook `getStudentGradebook` self-or-staff + `getGroupGradebook` staff-or-`gradebook_membership` → Task 4. ✓
- files `FileAccessAuthorizer` SPI by `ReferenceType`; homework owner-or-staff, chat room-member, files MATERIAL→permissive; `download` calls it → Tasks 5–7. ✓
- chat `isMember(roomId,userId)` + `main` `ChatSubscriptionInterceptor` on client-inbound SUBSCRIBE → Tasks 7–8. ✓
- **One negative-authz test per IDOR fixed** (other student → submission/attempt 404; other user → gradebook 404; non-member → HOMEWORK/CHAT file 404; non-member SUBSCRIBE rejected) → Tasks 2,3,4,7,8. ✓

**Decisions honored verbatim:** 404 (not 403) on mismatch; service-layer `findByIdForCaller`; `isStaff()` staff bypass; SPI resolved by `ReferenceType`; MATERIAL = any-authenticated; SUBSCRIBE-frame membership check. ✓

**Placeholder scan:** no TBD/TODO; every task has real code, real test bodies, exact paths, and a verify command. ✓

**Deviations (flagged, faithful to binding invariants):**
1. `FileAccessAuthorizer.canAccess` takes `(UUID referenceId, UUID uploaderId, UUID userId)` rather than the `StoredFile` entity — the root invariant forbids importing another module's `@Entity`. The SPI name and intent are unchanged.
2. The "non-member SUBSCRIBE is rejected" check is a unit test of `ChatSubscriptionInterceptor` (not a STOMP integration test) because the broker is `@Profile("!integration-test")` and absent under `BaseIntegrationTest`.
3. Adds the first intra-backend Maven deps (`homework → files`, `chat → files`) so those modules can implement the SPI — required by the spec's "implementers in homework/chat" and consistent with each module's documented "depends on files" (homework already; chat's contract updated in Task 7).
