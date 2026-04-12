package dn.orderservice.dto.response;

import dn.orderservice.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Schema(description = "Результат создания/получения заказа")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    @Schema(description = "UUID заказа", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID orderId;

    @Schema(description = "Текущий статус заказа", example = "PENDING",
            allowableValues = {"PENDING", "CONFIRMED", "PAID", "CANCELED"})
    private OrderStatus orderStatus;
}
