package app.mata.gradup.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GraduationEligibility(
    UUID studentId,
    Track track,
    boolean isEligible,
    BigDecimal overallAverage,
    List<FailingCourse> failingCourses) {}
