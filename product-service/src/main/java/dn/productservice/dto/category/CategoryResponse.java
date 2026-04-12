package dn.productservice.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Данные категории товаров")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CategoryResponse {

    @Schema(description = "UUID категории", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String id;

    @Schema(description = "Название категории", example = "Электроника")
    private String name;

    @Schema(description = "UUID родительской категории", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String parentCategoryId;
}
