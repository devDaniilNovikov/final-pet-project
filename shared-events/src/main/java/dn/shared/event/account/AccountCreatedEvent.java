package dn.shared.event.account;

import lombok.Builder;

import java.util.UUID;
@Builder
public record AccountCreatedEvent(
        UUID accountId,
        UUID eventId,
        String username
) {
}
