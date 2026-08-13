package app.mata.gradup.model;

import java.util.UUID;

public record User(
    UUID id,
    String reference,
    String lastName,
    String firstName,
    String email,
    String phone,
    Role role,
    boolean isActive) {}
