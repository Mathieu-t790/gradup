package app.mata.gradup.model;

import java.time.LocalDate;
import java.util.UUID;

public record Semester(
    UUID id, int number, AcademicYear academicYear, LocalDate startDate, LocalDate endDate) {}
