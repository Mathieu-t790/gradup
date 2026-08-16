package app.mata.gradup.service.utils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class Ranking {

  private Ranking() {}

  public static <T> List<T> sortByAverageDesc(
      List<T> items, Function<T, BigDecimal> averageOf, Function<T, UUID> idOf) {
    return items.stream()
        .sorted(
            Comparator.comparing(averageOf, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(idOf, Comparator.comparing(UUID::toString)))
        .toList();
  }

  public static <T> Map<UUID, Integer> competitionRanks(
      List<T> sorted, Function<T, BigDecimal> averageOf, Function<T, UUID> idOf) {
    Map<UUID, Integer> ranks = new HashMap<>();
    int rank = 0;
    BigDecimal previousAverage = null;
    for (int i = 0; i < sorted.size(); i++) {
      T item = sorted.get(i);
      BigDecimal average = averageOf.apply(item);
      if (previousAverage == null || !sameAverage(previousAverage, average)) {
        rank = i + 1;
      }
      ranks.put(idOf.apply(item), rank);
      previousAverage = average;
    }
    return ranks;
  }

  private static boolean sameAverage(BigDecimal a, BigDecimal b) {
    if (a == null && b == null) {
      return true;
    }
    return a != null && b != null && a.compareTo(b) == 0;
  }
}
