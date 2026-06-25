package de.codillas.chat.domain.model;

/** Kind of chat room: a group channel, a 1:1 direct message, or a per-assignment thread. */
public enum ChatRoomType {
  GROUP,
  DIRECT,
  ASSIGNMENT_THREAD
}
