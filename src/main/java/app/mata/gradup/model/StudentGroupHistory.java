package app.mata.gradup.model;

import java.time.LocalDate;
import java.util.UUID;

public record StudentGroupHistory(
    UUID id, Group group, LocalDate startDate, LocalDate endDate, String changeReason) {}
