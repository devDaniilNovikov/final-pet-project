package dn.productservice.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "name of category can't be blak")
    private String name;

    private UUID parentCategoryId;
}
