package app.mata.gradup.model;

import java.time.Instant;
import java.util.UUID;

public record SemesterCreditValidation(
    UUID id,
    Semester semester,
    Track track,
    int totalCredits,
    Instant validatedAt,
    String validatedByName) {}
