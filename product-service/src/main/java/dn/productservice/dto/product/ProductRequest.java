package dn.productservice.dto.product;

import dn.productservice.dto.image.ProductImageRequest;
import dn.productservice.entity.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Запрос на создание/обновление товара")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductRequest {

    @Schema(description = "Название товара", example = "Смартфон Samsung Galaxy S25", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "name of product can't be blank")
    private String name;

    @Schema(description = "Описание товара", example = "Флагманский смартфон с AMOLED-экраном")
    private String description;

    @Schema(description = "Цена товара", example = "79999.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "price can't be null")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @Schema(description = "Количество на складе", example = "50")
    @Min(0)
    private Integer quantity;

    @Schema(description = "Статус товара", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "product status can't be null")
    private ProductStatus status;

    @Schema(description = "UUID продавца", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "sellerId can't be null")
    private UUID sellerId;

    @Schema(description = "UUID категории товара", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID categoryId;

    @Schema(description = "Список изображений товара")
    @Valid
    private List<ProductImageRequest> images;
}
