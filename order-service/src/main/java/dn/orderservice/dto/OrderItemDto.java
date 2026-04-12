package dn.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Позиция заказа с деталями товара")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

    @Schema(description = "UUID товара", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID productId;

    @Schema(description = "Название товара", example = "Смартфон Samsung Galaxy S25")
    private String productName;

    @Schema(description = "Количество", example = "2")
    private int quantity;

    @Schema(description = "Цена за единицу", example = "79999.99")
    private BigDecimal price;
}
