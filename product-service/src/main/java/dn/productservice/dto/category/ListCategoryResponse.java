package dn.productservice.dto.category;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ListCategoryResponse {

    private List<CategoryResponse> categories;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
