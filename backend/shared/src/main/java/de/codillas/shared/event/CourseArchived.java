package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a course transitions to ARCHIVED. Lives in {@code shared}: enrollment consumes it
 * to mark the course no longer open for new groups.
 */
public record CourseArchived(UUID courseId) {}
