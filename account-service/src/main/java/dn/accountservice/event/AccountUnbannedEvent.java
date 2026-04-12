package dn.accountservice.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AccountUnbannedEvent(Instant unbannedDate,
                                   String id) {
}
