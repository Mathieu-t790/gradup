package app.mata.gradup.model;

import java.time.LocalDate;
import java.util.UUID;

public record StudentTrackHistory(
    UUID id, Track track, LocalDate startDate, LocalDate endDate, String changeReason) {}
