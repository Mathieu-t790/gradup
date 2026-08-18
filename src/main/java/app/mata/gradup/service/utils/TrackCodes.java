package app.mata.gradup.service.utils;

import app.mata.gradup.model.TrackCode;

/** Shared {@link TrackCode} conversions between the domain and the generated REST model. */
public final class TrackCodes {

  private TrackCodes() {}

  public static app.mata.gradup.endpoint.rest.model.TrackCode toRest(TrackCode code) {
    return code == null ? null : app.mata.gradup.endpoint.rest.model.TrackCode.valueOf(code.name());
  }

  /** Parses a raw query parameter into a REST {@code TrackCode} (blank and null mean no filter). */
  public static app.mata.gradup.endpoint.rest.model.TrackCode toRest(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return app.mata.gradup.endpoint.rest.model.TrackCode.fromValue(raw);
  }

  public static TrackCode toDomain(app.mata.gradup.endpoint.rest.model.TrackCode code) {
    return code == null ? null : TrackCode.valueOf(code.name());
  }
}
