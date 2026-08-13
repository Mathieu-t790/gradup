package app.mata.gradup.model;

import java.util.UUID;

public record Course(
    UUID id, String reference, String title, int credits, int semesterNumber, Track track) {}
