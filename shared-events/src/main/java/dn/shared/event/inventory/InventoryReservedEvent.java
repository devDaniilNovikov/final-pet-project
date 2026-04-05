package dn.shared.event.inventory;

import lombok.Builder;

@Builder
public record InventoryReservedEvent(String orderId) {
}
