package app.mata.gradup.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RankingTest {

  private record Candidate(UUID id, BigDecimal average) {}

  private static Candidate candidate(String seed, BigDecimal average) {
    return new Candidate(UUID.fromString("00000000-0000-0000-0000-" + seed), average);
  }

  @Test
  void sortByAverageDesc_orders_highest_first_and_keeps_null_averages_last() {
    Candidate high = candidate("000000000001", new BigDecimal("15.00"));
    Candidate mid = candidate("000000000002", new BigDecimal("12.00"));
    Candidate low = candidate("000000000003", new BigDecimal("10.00"));
    Candidate noAverage = candidate("000000000004", null);

    List<Candidate> sorted =
        Ranking.sortByAverageDesc(
            List.of(low, noAverage, high, mid), Candidate::average, Candidate::id);

    assertEquals(List.of(high.id(), mid.id(), low.id(), noAverage.id()), ids(sorted));
  }

  @Test
  void competitionRanks_gives_the_same_rank_to_ties_and_skips_the_next_ranks() {
    Candidate first = candidate("000000000001", new BigDecimal("15.00"));
    Candidate tiedA = candidate("000000000002", new BigDecimal("14.00"));
    Candidate tiedB = candidate("000000000003", new BigDecimal("14.00"));
    Candidate fourth = candidate("000000000004", new BigDecimal("12.00"));

    List<Candidate> sorted =
        Ranking.sortByAverageDesc(
            List.of(tiedB, fourth, first, tiedA), Candidate::average, Candidate::id);
    Map<UUID, Integer> ranks = Ranking.competitionRanks(sorted, Candidate::average, Candidate::id);

    assertEquals(1, ranks.get(first.id()));
    assertEquals(2, ranks.get(tiedA.id()));
    assertEquals(2, ranks.get(tiedB.id()));
    assertEquals(4, ranks.get(fourth.id()));
  }

  @Test
  void competitionRanks_ranks_null_averages_last() {
    Candidate graded = candidate("000000000001", new BigDecimal("11.00"));
    Candidate noAverageA = candidate("000000000002", null);
    Candidate noAverageB = candidate("000000000003", null);

    List<Candidate> sorted =
        Ranking.sortByAverageDesc(
            List.of(noAverageB, graded, noAverageA), Candidate::average, Candidate::id);
    Map<UUID, Integer> ranks = Ranking.competitionRanks(sorted, Candidate::average, Candidate::id);

    assertEquals(1, ranks.get(graded.id()));
    assertEquals(2, ranks.get(noAverageA.id()));
    assertEquals(3, ranks.get(noAverageB.id()));
  }

  @Test
  void competitionRanks_orders_equal_averages_by_id_for_determinism() {
    Candidate sameA = candidate("000000000001", new BigDecimal("13.00"));
    Candidate sameB = candidate("000000000002", new BigDecimal("13.00"));

    List<Candidate> sorted =
        Ranking.sortByAverageDesc(List.of(sameB, sameA), Candidate::average, Candidate::id);

    assertEquals(List.of(sameA.id(), sameB.id()), ids(sorted));
    Map<UUID, Integer> ranks = Ranking.competitionRanks(sorted, Candidate::average, Candidate::id);
    assertEquals(1, ranks.get(sameA.id()));
    assertEquals(1, ranks.get(sameB.id()));
  }

  private static List<UUID> ids(List<Candidate> candidates) {
    return candidates.stream().map(Candidate::id).toList();
  }
}
