package com.studentportal.common.util;

import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginationUtils {

  private static final int MAX_PAGE_SIZE = 100;

  /**
   * Builds a {@link PageRequest} from optional query params. Sorting is limited to {@code
   * allowedSortFields} so clients cannot sort on arbitrary columns; unknown values fall back to
   * {@code createdAt}.
   */
  public static PageRequest getPageRequest(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortDirection,
      Optional<String> sortBy,
      Set<String> allowedSortFields) {
    String field = sortBy.filter(allowedSortFields::contains).orElse("createdAt");
    Sort.Direction direction =
        sortDirection.flatMap(Sort.Direction::fromOptionalString).orElse(Sort.Direction.DESC);
    int pageSize = Math.min(Math.max(size.orElse(10), 1), MAX_PAGE_SIZE);
    return PageRequest.of(Math.max(page.orElse(0), 0), pageSize, Sort.by(direction, field));
  }
}
