package de.codillas.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ShuffleOrder")
class ShuffleOrderTest {

  private static final List<Integer> ITEMS = List.of(1, 2, 3, 4, 5, 6, 7, 8);

  @Test
  @DisplayName("same seed yields the same order and the same multiset")
  void deterministic() {
    UUID seed = UUID.randomUUID();
    List<Integer> a = ShuffleOrder.seededBy(seed).apply(ITEMS);
    List<Integer> b = ShuffleOrder.seededBy(seed).apply(ITEMS);
    assertThat(a).isEqualTo(b).containsExactlyInAnyOrderElementsOf(ITEMS);
  }

  @Test
  @DisplayName("different seeds (or combined keys) generally produce different orders")
  void variesBySeed() {
    List<Integer> a = ShuffleOrder.seededBy(UUID.randomUUID()).apply(ITEMS);
    List<Integer> b = ShuffleOrder.seededBy(UUID.randomUUID()).apply(ITEMS);
    List<Integer> combined =
        ShuffleOrder.seededBy(UUID.randomUUID()).combine(UUID.randomUUID()).apply(ITEMS);
    assertThat(a).isNotEqualTo(b);
    assertThat(combined).containsExactlyInAnyOrderElementsOf(ITEMS);
  }

  @Test
  @DisplayName("apply does not mutate the input list")
  void pure() {
    List<Integer> input = List.of(1, 2, 3);
    ShuffleOrder.seededBy(UUID.randomUUID()).apply(input);
    assertThat(input).containsExactly(1, 2, 3);
  }
}
