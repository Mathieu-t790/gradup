package app.mata.gradup.model;

import java.util.List;
import java.util.UUID;

public record CourseOffering(
    UUID id,
    Course course,
    Group group,
    Semester semester,
    List<Teacher> teachers,
    boolean gradingFinalized) {}
