package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a chat message is posted. Lives in {@code shared} because it is consumed outside
 * chat (notification alerts offline room members).
 */
public record MessagePosted(UUID roomId, UUID messageId, UUID senderId) {}
