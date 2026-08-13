package app.mata.gradup.model;

import java.util.UUID;

public record Cohort(UUID id, String label, int entryYear, int expectedGraduationYear) {}
