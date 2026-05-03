package dn.shared.event.account;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AccountUnbannedEvent(UUID eventId,
                                   Instant unbannedDate,
                                   UUID id) {
}
