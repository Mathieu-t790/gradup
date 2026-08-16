package app.mata.gradup.service.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class Pages {

  public static final int DEFAULT_PAGE_SIZE = 200;

  private Pages() {}

  public static <T> Page<T> subPage(List<T> content, Pageable pageable) {
    long total = content.size();
    int from = (int) Math.min(pageable.getOffset(), total);
    int to = (int) Math.min(from + pageable.getPageSize(), total);
    return new PageImpl<>(content.subList(from, to), pageable, total);
  }

  public static <T> List<T> allPages(Function<Pageable, Page<T>> fetcher, int pageSize) {
    List<T> all = new ArrayList<>();
    int pageNumber = 0;
    Page<T> page;
    do {
      page = fetcher.apply(PageRequest.of(pageNumber, pageSize));
      all.addAll(page.getContent());
      pageNumber++;
    } while (page.hasNext());
    return all;
  }
}
