package de.codillas.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;

import de.codillas.assessment.domain.model.AttemptStatus;
import de.codillas.assessment.domain.model.QuestionType;
import de.codillas.assessment.domain.model.TestStatus;
import de.codillas.chat.domain.model.ChatRoomType;
import de.codillas.course.domain.model.CourseStatus;
import de.codillas.course.domain.model.MaterialType;
import de.codillas.enrollment.domain.model.GroupStatus;
import de.codillas.files.domain.model.FileReferenceType;
import de.codillas.gradebook.domain.model.GradeSource;
import de.codillas.homework.domain.model.AssignmentStatus;
import de.codillas.homework.domain.model.SubmissionStatus;
import de.codillas.notification.domain.model.NotificationType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the enum vocabularies that the 0.7.0 CHECK constraints hand-copy into SQL string lists.
 *
 * <p>Each {@code ck_*} constraint listing an enum's constants is a second, untyped copy of a Java
 * enum. Nothing in the compiler ties the two together, so adding a constant is a silent production
 * break: the row is rejected by Postgres on a path whose unit tests are all mocked and green. This
 * test reads each constraint's real definition out of {@code pg_catalog} and diffs it against the
 * enum's constants, so the drift fails the build instead of a user's request.
 */
@DisplayName("CHECK constraint enum lists match their Java enums")
class EnumCheckConstraintTest extends BaseIntegrationTest {

  /**
   * Constraint name → the enum it must mirror.
   *
   * <p>ponytail: hand-maintained map. It catches a new constant in an already-listed enum (the
   * failure that shipped twice in 0.7.0), but a brand-new enum column whose constraint is never
   * added here is still unguarded. Upgrade path if that bites: scan {@code @Enumerated(STRING)}
   * fields off the Hibernate metamodel and resolve table/column automatically.
   *
   * <p>Deliberately absent: {@code course_status_view}. It is enrollment's projection of course's
   * vocabulary, stored as a raw String, and carries no CHECK by design — see
   * 0.7.0-add-enrollment-checks.yaml.
   */
  private static final Map<String, Class<? extends Enum<?>>> ENUM_CONSTRAINTS =
      new LinkedHashMap<>();

  static {
    ENUM_CONSTRAINTS.put("ck_tests_status", TestStatus.class);
    ENUM_CONSTRAINTS.put("ck_questions_type", QuestionType.class);
    ENUM_CONSTRAINTS.put("ck_attempts_status", AttemptStatus.class);
    ENUM_CONSTRAINTS.put("ck_courses_status", CourseStatus.class);
    ENUM_CONSTRAINTS.put("ck_materials_type", MaterialType.class);
    ENUM_CONSTRAINTS.put("ck_study_groups_status", GroupStatus.class);
    ENUM_CONSTRAINTS.put("ck_stored_files_reference_type", FileReferenceType.class);
    ENUM_CONSTRAINTS.put("ck_progress_entries_source", GradeSource.class);
    ENUM_CONSTRAINTS.put("ck_assignments_status", AssignmentStatus.class);
    ENUM_CONSTRAINTS.put("ck_submissions_status", SubmissionStatus.class);
    ENUM_CONSTRAINTS.put("ck_chat_rooms_type", ChatRoomType.class);
    ENUM_CONSTRAINTS.put("ck_notifications_type", NotificationType.class);
  }

  /** Matches the single-quoted literals Postgres prints in a constraint definition. */
  private static final Pattern LITERAL = Pattern.compile("'([A-Z_]+)'");

  @Autowired private DataSource dataSource;

  @Test
  @DisplayName("every ck_* enum list is exactly its Java enum's constants")
  void checkConstraintsMatchEnums() throws Exception {
    var mismatches = new LinkedHashMap<String, String>();

    for (var entry : ENUM_CONSTRAINTS.entrySet()) {
      String constraint = entry.getKey();
      Set<String> allowed = allowedValues(constraint);
      Set<String> constants =
          Stream.of(entry.getValue().getEnumConstants())
              .map(Enum::name)
              .collect(Collectors.toCollection(TreeSet::new));

      if (allowed == null) {
        mismatches.put(constraint, "constraint not found in pg_catalog");
      } else if (!allowed.equals(constants)) {
        mismatches.put(
            constraint,
            "SQL allows %s but %s declares %s"
                .formatted(allowed, entry.getValue().getSimpleName(), constants));
      }
    }

    assertThat(mismatches)
        .describedAs(
            "CHECK constraint enum lists drifted from their Java enums — update the 0.7.0 changelog"
                + " for each entry below")
        .isEmpty();
  }

  /** The values a CHECK constraint admits, or null when no such constraint exists. */
  private Set<String> allowedValues(String constraintName) throws Exception {
    try (var connection = dataSource.getConnection();
        var statement =
            connection.prepareStatement(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = ?")) {
      statement.setString(1, constraintName);
      try (ResultSet rs = statement.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        Matcher matcher = LITERAL.matcher(rs.getString(1));
        var values = new TreeSet<String>();
        while (matcher.find()) {
          values.add(matcher.group(1));
        }
        return values;
      }
    }
  }
}
