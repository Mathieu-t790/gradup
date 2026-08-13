package app.mata.gradup.endpoint.event.consumer.model;

import app.mata.gradup.PojaGenerated;
import app.mata.gradup.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
