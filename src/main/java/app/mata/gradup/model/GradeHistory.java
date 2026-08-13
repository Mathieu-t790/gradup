package app.mata.gradup.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GradeHistory(
    UUID id,
    BigDecimal oldScore,
    BigDecimal newScore,
    String modifiedByName,
    Instant modifiedAt,
    String reason) {}
