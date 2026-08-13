package app.mata.gradup.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transcript(
    UUID id,
    UUID studentId,
    TranscriptType type,
    UUID semesterId,
    UUID academicYearId,
    UUID diplomaId,
    BigDecimal overallAverage,
    Integer creditsEarned,
    Instant generatedAt,
    String downloadUrl,
    Instant sentAt,
    String recipientEmail) {}
