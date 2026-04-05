package dn.productservice.dto.product;

import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ListProductResponse {

    private List<ProductResponse> products;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
