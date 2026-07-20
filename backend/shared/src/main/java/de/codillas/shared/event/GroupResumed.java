package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when an ARCHIVED cohort is put back into RUNNING. The counterpart of {@link
 * GroupArchived}: homework consumes it to let deadline reminders flow again.
 */
public record GroupResumed(UUID groupId) {}
