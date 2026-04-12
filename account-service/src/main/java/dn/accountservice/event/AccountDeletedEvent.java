package dn.accountservice.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AccountDeletedEvent(String id, Instant deletedDate) {
}
