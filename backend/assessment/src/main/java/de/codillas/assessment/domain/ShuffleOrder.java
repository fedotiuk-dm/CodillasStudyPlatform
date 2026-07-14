package de.codillas.assessment.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Deterministic, seedable ordering for the taker view. Seeded by an attempt id so the order is
 * stable across reloads yet differs per student; {@link #combine(UUID)} mixes in a question id so
 * each question's options shuffle independently. Pure — no Spring, no correctness data.
 */
public final class ShuffleOrder {

  /** FNV-1a 64-bit prime — mixes the parent seed before folding in a child id. */
  private static final long FNV_PRIME = 1099511628211L;

  private final long seed;

  private ShuffleOrder(long seed) {
    this.seed = seed;
  }

  public static ShuffleOrder seededBy(UUID id) {
    return new ShuffleOrder(mix(id));
  }

  /** A child order keyed by {@code id} (e.g. a question id) — distinct from the parent. */
  public ShuffleOrder combine(UUID id) {
    return new ShuffleOrder(seed * FNV_PRIME ^ mix(id));
  }

  /** A new list holding {@code items} in this order; the input is never mutated. */
  public <T> List<T> apply(List<T> items) {
    List<T> copy = new ArrayList<>(items);
    // Seeded Random gives a stable, reproducible order. Not security — it only arranges quiz
    // questions/options on screen, so the weak-PRNG warning (S2245) is a knowing false positive.
    Collections.shuffle(copy, new Random(seed));
    return copy;
  }

  private static long mix(UUID id) {
    return id.getMostSignificantBits() ^ id.getLeastSignificantBits();
  }
}
