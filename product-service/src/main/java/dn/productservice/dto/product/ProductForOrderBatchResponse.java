package dn.productservice.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Данные товара из product-service (внутренний DTO)")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductForOrderBatchResponse {

    @Schema(description = "UUID товара", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID productId;

    @Schema(description = "Цена товара", example = "79999.99")
    private BigDecimal price;

    @Schema(description = "Название товара", example = "Смартфон Samsung Galaxy S25")
    private String productName;

    @Schema(description = "Доступное количество на складе", example = "50")
    private Integer quantity;
}