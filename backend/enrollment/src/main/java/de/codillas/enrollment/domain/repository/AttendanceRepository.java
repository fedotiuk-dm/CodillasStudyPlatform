package de.codillas.enrollment.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.enrollment.domain.model.Attendance;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

  Optional<Attendance> findByScheduledLessonIdAndUserId(UUID scheduledLessonId, UUID userId);
}
