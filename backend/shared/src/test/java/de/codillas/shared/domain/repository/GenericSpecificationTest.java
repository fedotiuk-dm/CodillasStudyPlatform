package de.codillas.shared.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.codillas.shared.domain.Sortable;

import lombok.Setter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenericSpecification")
class GenericSpecificationTest {

  @Setter
  static class Item implements Sortable {
    private int sortOrder;

    Item(int sortOrder) {
      this.sortOrder = sortOrder;
    }

    @Override
    public int getSortOrder() {
      return sortOrder;
    }
  }

  @Mock private JpaSpecificationExecutor<Item> executor;

  @Test
  @DisplayName("getNextSortOrder returns max + 1 when rows exist")
  void getNextSortOrder_withRows() {
    when(executor.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(new Item(7))));
    assertThat(GenericSpecification.getNextSortOrder(executor)).isEqualTo(8);
  }

  @Test
  @DisplayName("getNextSortOrder starts at 1 when there are no rows")
  void getNextSortOrder_empty() {
    when(executor.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
    assertThat(GenericSpecification.getNextSortOrder(executor)).isEqualTo(1);
  }

  @Test
  @DisplayName("withDefaultSort applies the default only when the pageable is unsorted")
  void withDefaultSort_appliesWhenUnsorted() {
    Sort byName = Sort.by("name");
    assertThat(GenericSpecification.withDefaultSort(PageRequest.of(0, 20), byName).getSort())
        .isEqualTo(byName);

    Sort existing = Sort.by("createdAt");
    Pageable sorted = PageRequest.of(0, 20, existing);
    assertThat(GenericSpecification.withDefaultSort(sorted, byName).getSort()).isEqualTo(existing);
  }
}
