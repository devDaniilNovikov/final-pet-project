package dn.productservice.dto.image;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductImageResponse {

    private String id;
    private String url;
    private Integer displayOrder;
}
