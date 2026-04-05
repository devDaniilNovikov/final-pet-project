package dn.productservice.dto.product;

import dn.productservice.dto.category.CategoryResponse;
import dn.productservice.dto.image.ProductImageResponse;
import dn.productservice.entity.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductResponse {

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private ProductStatus status;
    private String sellerId;
    private CategoryResponse category;
    private List<ProductImageResponse> images;
    private Instant createdAt;
    private Instant updatedAt;
}
