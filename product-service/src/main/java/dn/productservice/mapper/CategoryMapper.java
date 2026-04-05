package dn.productservice.mapper;

import dn.productservice.dto.category.CategoryRequest;
import dn.productservice.dto.category.CategoryResponse;
import dn.productservice.dto.category.ListCategoryResponse;
import dn.productservice.entity.CategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(source = "parentCategory.id", target = "parentCategoryId")
    CategoryResponse toResponse(CategoryEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentCategory", ignore = true)
    @Mapping(target = "subCategories", ignore = true)
    @Mapping(target = "products", ignore = true)
    CategoryEntity toEntity(CategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentCategory", ignore = true)
    @Mapping(target = "subCategories", ignore = true)
    @Mapping(target = "products", ignore = true)
    void updateEntity(CategoryRequest request, @MappingTarget CategoryEntity entity);

    default ListCategoryResponse toListResponse(Page<CategoryEntity> entities) {
        ListCategoryResponse listCategoryResponse = new ListCategoryResponse();
        listCategoryResponse.setCategories(entities.stream()
                .map(this::toResponse)
                .toList());
        listCategoryResponse.setTotalElements(entities.getTotalElements());
        listCategoryResponse.setTotalPages(entities.getTotalPages());
        listCategoryResponse.setCurrentPage(entities.getNumber());
        return listCategoryResponse;
    }
}
