package dn.shared.event.account;

import dn.shared.outbox.OutboxableEvent;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AccountBannedEvent(UUID id,
                                 UUID eventId,
                                 Instant unbanDate,
                                 String reason) implements OutboxableEvent {
}
