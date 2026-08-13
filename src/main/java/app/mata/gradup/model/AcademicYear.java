package app.mata.gradup.model;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicYear(UUID id, String label, LocalDate startDate, LocalDate endDate) {}
