package dn.productservice.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Schema(description = "Постраничный список товаров")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ListProductResponse {

    @Schema(description = "Список товаров")
    private List<ProductResponse> products;

    @Schema(description = "Общее количество элементов", example = "100")
    private long totalElements;

    @Schema(description = "Общее количество страниц", example = "10")
    private int totalPages;

    @Schema(description = "Текущая страница (с 0)", example = "0")
    private int currentPage;
}
