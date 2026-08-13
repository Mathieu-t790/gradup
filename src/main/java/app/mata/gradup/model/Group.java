package app.mata.gradup.model;

import java.util.UUID;

public record Group(UUID id, String reference, Cohort cohort, Track track) {}
