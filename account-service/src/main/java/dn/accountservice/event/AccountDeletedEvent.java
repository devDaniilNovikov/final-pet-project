package dn.accountservice.event;

import lombok.Builder;

import java.time.Instant;

@Builder
public record AccountDeletedEvent(String id, Instant deletedDate) {
}
