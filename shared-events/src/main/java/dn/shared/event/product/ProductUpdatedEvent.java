package dn.shared.event.product;

import dn.shared.outbox.OutboxableEvent;
import lombok.Builder;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;


@Builder
public record ProductUpdatedEvent(UUID id,
                                  UUID eventId,
                                  String name,
                                  Instant updatedTime) implements OutboxableEvent, Serializable {
}
