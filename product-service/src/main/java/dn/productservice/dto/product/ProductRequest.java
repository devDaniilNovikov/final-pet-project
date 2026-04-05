package dn.productservice.dto.product;

import dn.productservice.dto.image.ProductImageRequest;
import dn.productservice.entity.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "name of product can't be blak")
    private String name;

    private String description;

    @NotNull(message = "price can't be null")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @Min(0)
    private Integer quantity;

    @NotNull(message = "product status can't be null")
    private ProductStatus status;

    @NotNull(message = "sellerId can't be null")
    private UUID sellerId;

    private UUID categoryId;

    @Valid
    private List<ProductImageRequest> images;
}
