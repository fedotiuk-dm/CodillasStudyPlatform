package de.codillas.gradebook.mapper;

import java.util.List;
import java.util.UUID;

import de.codillas.gradebook.api.dto.CourseGradeResponse;
import de.codillas.gradebook.api.dto.GroupGradebookResponse;
import de.codillas.gradebook.api.dto.ProgressEntryResponse;
import de.codillas.gradebook.api.dto.StudentGradebookResponse;
import de.codillas.gradebook.api.dto.TypeGradeResponse;
import de.codillas.gradebook.domain.TypeGrade;
import de.codillas.gradebook.domain.WeightedGrade;
import de.codillas.gradebook.domain.model.GradebookMembership;
import de.codillas.gradebook.domain.model.ProgressEntry;
import de.codillas.shared.event.AttemptCompleted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.event.SubmissionGraded;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface GradebookMapper {

  @Mapping(target = "recordedAt", source = "createdAt")
  ProgressEntryResponse toEntryResponse(ProgressEntry entry);

  List<ProgressEntryResponse> toEntryResponses(List<ProgressEntry> entries);

  @Mapping(target = "percent", expression = "java(type.percent())")
  TypeGradeResponse toTypeGrade(TypeGrade type);

  @Mapping(target = "percent", expression = "java(grade.percent())")
  CourseGradeResponse toCourseGrade(WeightedGrade grade);

  StudentGradebookResponse toStudentGradebook(
      UUID studentId, List<ProgressEntryResponse> entries, CourseGradeResponse courseGrade);

  GroupGradebookResponse toGroupGradebook(UUID groupId, List<StudentGradebookResponse> students);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "source", constant = "HOMEWORK")
  @Mapping(target = "sourceId", source = "submissionId")
  @Mapping(target = "referenceId", source = "assignmentId")
  @Mapping(target = "score", source = "awarded")
  @Mapping(target = "maxPoints", source = "maxPoints")
  @Mapping(target = "groupId", source = "groupId")
  ProgressEntry toEntry(SubmissionGraded event);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "source", constant = "TEST")
  @Mapping(target = "sourceId", source = "attemptId")
  @Mapping(target = "referenceId", source = "testId")
  @Mapping(target = "score", source = "awarded")
  @Mapping(target = "maxPoints", source = "maxPoints")
  @Mapping(target = "groupId", source = "groupId")
  ProgressEntry toEntry(AttemptCompleted event);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "studentId", source = "userId")
  GradebookMembership toMembership(StudentEnrolled event);
}
