package de.codillas.shared.domain;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import de.codillas.shared.exception.ConflictException;

import lombok.experimental.UtilityClass;

/**
 * Shared transition logic for the per-aggregate {@code <Aggregate>StateMachine} beans (see
 * backend/AGENTS.md). Each bean keeps its own declarative {@code ALLOWED} map and any
 * domain-specific guards; this only owns the common "is this move allowed? else 409" algorithm and
 * its error-message contract.
 */
@UtilityClass
public class StateMachines {

  /**
   * Applies {@code target} via {@code setter} when {@code allowed} permits moving from {@code
   * current}; otherwise throws a {@link ConflictException}.
   *
   * @param aggregate lowercase aggregate noun for the error message (e.g. {@code "attempt"})
   */
  public static <S extends Enum<S>> void transition(
      String aggregate, S current, S target, Map<S, Set<S>> allowed, Consumer<S> setter) {
    if (!allowed.getOrDefault(current, Set.of()).contains(target)) {
      throw new ConflictException(
          "Cannot move " + aggregate + " from " + current + " to " + target);
    }
    setter.accept(target);
  }
}
