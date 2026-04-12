package dn.productservice.dto.image;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Schema(description = "Запрос на добавление изображения товара")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductImageRequest {

    @Schema(description = "URL изображения", example = "https://cdn.example.com/products/image1.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "url of product image can't be blank")
    private String url;

    @Schema(description = "Порядок отображения", example = "0")
    @Min(0)
    private Integer displayOrder;
}
