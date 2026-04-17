package dn.shared.event.inventory;

import lombok.Builder;

import java.util.UUID;

@Builder
public record InventoryReservedEvent(UUID eventId,
                                     UUID orderId) {
}
