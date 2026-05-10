package dn.shared.event.account;

import dn.shared.outbox.OutboxableEvent;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AccountUpdatedEvent(UUID eventId,
                                  UUID id,
                                  Instant updatedTime) implements OutboxableEvent {}
