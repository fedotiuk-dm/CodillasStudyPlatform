package de.codillas.enrollment.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.enrollment.domain.model.CourseStatusView;

/** Enrollment's local read model of course lifecycle status, fed by course events. */
@Repository
public interface CourseStatusViewRepository extends JpaRepository<CourseStatusView, UUID> {}
