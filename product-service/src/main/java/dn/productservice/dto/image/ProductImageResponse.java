package dn.productservice.dto.image;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Данные изображения товара")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductImageResponse {

    @Schema(description = "UUID изображения", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String id;

    @Schema(description = "URL изображения", example = "https://cdn.example.com/products/image1.jpg")
    private String url;

    @Schema(description = "Порядок отображения", example = "0")
    private Integer displayOrder;
}
