package dn.shared.event.product;


import dn.shared.outbox.OutboxableEvent;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Builder
public record ProductDeletedEvent(UUID id,
                                  UUID eventId,
                                  String productName) implements OutboxableEvent, Serializable {
}
