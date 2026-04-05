package dn.productservice.event;

import lombok.Builder;

import java.time.Instant;


@Builder
public record ProductUpdatedEvent(String id, String name, Instant updatedTime) {
}
