package app.mata.gradup.model;

import java.time.Instant;
import java.util.UUID;

public record GradeDispute(
    UUID id,
    UUID gradeId,
    UUID studentId,
    String studentName,
    String courseReference,
    String examLabel,
    String reason,
    DisputeStatus status,
    Instant createdAt,
    Instant resolvedAt,
    String resolvedByName,
    String resolutionNote,
    UUID resultingHistoryId) {}
