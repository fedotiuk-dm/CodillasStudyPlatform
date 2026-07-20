package de.codillas.shared.event;

import java.util.List;
import java.util.UUID;

/**
 * Published when lessons are hard-deleted — directly, or as part of a deleted section/course. Lives
 * in {@code shared}: enrollment, homework and assessment all hold a nullable {@code lessonId} and
 * clear it on receipt.
 *
 * <p>Batched rather than one event per lesson because deleting a course removes the whole tree in a
 * single {@code ON DELETE CASCADE} — the ids have to be collected up front, before the delete.
 */
public record LessonsDeleted(List<UUID> lessonIds) {}
