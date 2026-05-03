package dn.shared.event.account;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AccountDeletedEvent(UUID eventId,
                                  UUID id,
                                  String username,
                                  Instant deletedDate) {
}
