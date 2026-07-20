package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a stored file is hard-deleted. Lives in {@code shared}: course consumes it to drop
 * the materials that pointed at the file.
 */
public record FileDeleted(UUID fileId) {}
