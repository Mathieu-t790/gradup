package app.mata.gradup.model;

import java.time.LocalDate;

public record Student(
    User user,
    LocalDate dateOfBirth,
    Cohort cohort,
    Group currentGroup,
    Track currentTrack,
    LocalDate enrollmentDate) {}
