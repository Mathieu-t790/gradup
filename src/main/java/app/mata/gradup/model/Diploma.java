package app.mata.gradup.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Diploma(
    UUID id,
    Student student,
    Cohort cohort,
    Track track,
    BigDecimal overallAverage,
    int rank,
    LocalDate graduationDate,
    Instant listGeneratedAt) {}
