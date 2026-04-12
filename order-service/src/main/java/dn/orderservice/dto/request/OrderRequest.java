package dn.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Schema(description = "Запрос на создание заказа")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @Schema(description = "UUID покупателя", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "buyerId can't be null")
    private UUID buyerId;

    @Schema(description = "Список позиций заказа", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "items can't be empty")
    private List<OrderItemRequest> items;
}
