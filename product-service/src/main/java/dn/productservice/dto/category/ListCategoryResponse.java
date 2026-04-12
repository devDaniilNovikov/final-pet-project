package dn.productservice.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Постраничный список категорий")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListCategoryResponse {

    @Schema(description = "Список категорий")
    private List<CategoryResponse> categories;

    @Schema(description = "Общее количество элементов", example = "25")
    private long totalElements;

    @Schema(description = "Общее количество страниц", example = "3")
    private int totalPages;

    @Schema(description = "Текущая страница (с 0)", example = "0")
    private int currentPage;
}
