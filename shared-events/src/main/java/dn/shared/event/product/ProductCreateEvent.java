package dn.shared.event.product;


import dn.shared.outbox.OutboxableEvent;
import lombok.Builder;

import java.io.Serializable;
import java.util.UUID;

@Builder
public record ProductCreateEvent(UUID id,
                                 UUID eventId,
                                 String name) implements OutboxableEvent , Serializable {
}
