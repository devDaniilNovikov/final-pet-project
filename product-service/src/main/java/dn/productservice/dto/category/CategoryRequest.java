package dn.productservice.dto.category;import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Schema(description = "Запрос на создание/обновление категории")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CategoryRequest {

    @Schema(description = "Название категории", example = "Электроника", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "name of category can't be blank")
    private String name;

    @Schema(description = "UUID родительской категории (для вложенных категорий)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID parentCategoryId;
}
