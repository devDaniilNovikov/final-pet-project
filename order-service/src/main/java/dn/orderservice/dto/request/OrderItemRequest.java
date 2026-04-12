package dn.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Schema(description = "Позиция заказа")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @Schema(description = "UUID товара", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "productId can't be null")
    private UUID productId;

    @Schema(description = "Количество единиц товара", example = "2", minimum = "1")
    @Min(1)
    private int quantity;
}
