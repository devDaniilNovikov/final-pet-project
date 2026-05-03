package dn.shared.event.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;


public record OrderItemDto(
        UUID eventId,
        UUID productId,
        String productName,
        BigDecimal price,
        Integer quantity) {}
