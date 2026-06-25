package de.codillas.shared.domain.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.codillas.shared.domain.Sortable;

import lombok.experimental.UtilityClass;

/** Shared JPA Specification helpers. Currently scoped to {@link Sortable} ordering support. */
@UtilityClass
public class GenericSpecification {

  /**
   * Returns the next available {@code sortOrder} value (current max + 1, starting from 1). Uses a
   * 1-row DESC paged query — no aggregate, no native SQL.
   */
  public static <T extends Sortable> int getNextSortOrder(JpaSpecificationExecutor<T> executor) {
    return executor
        .findAll(
            (Specification<T>) (_, _, cb) -> cb.conjunction(),
            PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, Sortable.SORT_ORDER)))
        .stream()
        .findFirst()
        .map(entity -> entity.getSortOrder() + 1)
        .orElse(1);
  }

  /** Returns a pageable with {@code defaultSort} applied when the incoming pageable has no sort. */
  public static Pageable withDefaultSort(Pageable pageable, Sort defaultSort) {
    if (pageable.getSort().isSorted()) {
      return pageable;
    }
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
  }
}
