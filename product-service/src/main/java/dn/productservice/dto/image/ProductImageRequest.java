package dn.productservice.dto.image;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductImageRequest {

    @NotBlank(message = "url of product image can't be blak")
    private String url;

    @Min(0)
    private Integer displayOrder;
}
