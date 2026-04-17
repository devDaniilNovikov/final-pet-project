package dn.shared.event.inventory;

import lombok.Builder;

import java.util.UUID;

@Builder
public record InventoryReservationFailedEvent(UUID eventId,
                                              UUID orderId,
                                              String reason) {
}
