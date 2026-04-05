package dn.accountservice.event;

import lombok.Builder;

import java.time.Instant;

@Builder
public record AccountBannedEvent(String id,
                                 Instant unbanDate,
                                 String reason) {
}
