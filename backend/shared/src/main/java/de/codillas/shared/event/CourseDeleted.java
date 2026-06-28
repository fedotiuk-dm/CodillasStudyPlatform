package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a course is hard-deleted. Lives in {@code shared}: enrollment consumes it to
 * cascade-delete the course's groups.
 */
public record CourseDeleted(UUID courseId) {}
