package app.mata.gradup.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record Exam(
    UUID id,
    UUID offeringId,
    String label,
    LocalDate examDate,
    LocalTime examTime,
    int weightNumerator,
    int weightDenominator) {}
