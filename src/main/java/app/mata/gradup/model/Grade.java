package app.mata.gradup.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Grade(
    UUID id,
    UUID studentId,
    String studentName,
    UUID examId,
    String examLabel,
    String courseReference,
    BigDecimal score,
    Instant recordedAt,
    String recordedByName) {}
