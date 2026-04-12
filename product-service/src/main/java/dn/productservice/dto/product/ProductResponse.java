package dn.productservice.dto.product;

import dn.productservice.dto.category.CategoryResponse;
import dn.productservice.dto.image.ProductImageResponse;
import dn.productservice.entity.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Данные товара")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductResponse {

    @Schema(description = "UUID товара", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String id;

    @Schema(description = "Название товара", example = "Смартфон Samsung Galaxy S25")
    private String name;

    @Schema(description = "Описание товара", example = "Флагманский смартфон с AMOLED-экраном")
    private String description;

    @Schema(description = "Цена товара", example = "79999.99")
    private BigDecimal price;

    @Schema(description = "Количество на складе", example = "50")
    private Integer quantity;

    @Schema(description = "Статус товара", example = "ACTIVE")
    private ProductStatus status;

    @Schema(description = "UUID продавца", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String sellerId;

    @Schema(description = "Категория товара")
    private CategoryResponse category;

    @Schema(description = "Изображения товара")
    private List<ProductImageResponse> images;

    @Schema(description = "Дата создания")
    private Instant createdAt;

    @Schema(description = "Дата обновления")
    private Instant updatedAt;
}
