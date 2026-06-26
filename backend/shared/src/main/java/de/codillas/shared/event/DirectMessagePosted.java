package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a message is posted to a DIRECT (1:1) room. Lives in {@code shared} because it is
 * consumed by notification, which alerts the single other recipient (no presence tracking needed
 * for a DM). Group/assignment-thread rooms do not emit this — only {@link MessagePosted}.
 */
public record DirectMessagePosted(UUID roomId, UUID recipientId, UUID senderId) {}
