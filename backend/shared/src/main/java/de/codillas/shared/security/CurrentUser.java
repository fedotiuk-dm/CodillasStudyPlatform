package de.codillas.shared.security;

import java.util.UUID;

/** Accessor for the currently authenticated user's identity. */
public interface CurrentUser {

  /** The Keycloak subject (user id) of the authenticated request. */
  UUID id();
}
