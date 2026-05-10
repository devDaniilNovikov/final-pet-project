package dn.shared.event.account;

import dn.shared.outbox.OutboxableEvent;
import lombok.Builder;

import java.util.UUID;
@Builder
public record AccountCreatedEvent(
        UUID id,
        UUID eventId,
        String username
)implements OutboxableEvent {
}
