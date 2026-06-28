package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a course transitions to PUBLISHED. Lives in {@code shared}: enrollment consumes it
 * to learn, by id, which courses are open for group creation — replacing the old synchronous read
 * port.
 */
public record CoursePublished(UUID courseId) {}
