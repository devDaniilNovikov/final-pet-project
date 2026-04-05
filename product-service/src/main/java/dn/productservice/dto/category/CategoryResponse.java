package dn.productservice.dto.category;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CategoryResponse {

    private String id;
    private String name;
    private String parentCategoryId;
}
