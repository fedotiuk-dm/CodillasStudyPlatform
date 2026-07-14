package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a teacher posts an announcement to a group. Lives in {@code shared} so that {@code
 * notification} can fan it out to the group's members without depending on the {@code announcement}
 * module.
 */
public record AnnouncementPosted(UUID announcementId, UUID groupId, String title) {}
