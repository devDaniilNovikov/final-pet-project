package dn.productservice.mapper;

import dn.productservice.dto.product.ListProductResponse;
import dn.productservice.dto.product.ProductRequest;
import dn.productservice.dto.product.ProductResponse;
import dn.productservice.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {CategoryMapper.class, ProductImageMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    @Mapping(source = "productImageEntities", target = "images")
    ProductResponse toResponse(ProductEntity entity);

    List<ProductResponse> toResponseList(List<ProductEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "productImageEntities", ignore = true)
    ProductEntity toEntity(ProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "productImageEntities", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget ProductEntity entity);

    default ListProductResponse toListResponse(Page<ProductEntity> entities) {
        ListProductResponse response = new ListProductResponse();
        response.setProducts(entities.stream()
                .map(this::toResponse)
                .toList());
        response.setTotalElements(entities.getTotalElements());
        response.setTotalPages(entities.getTotalPages());
        response.setCurrentPage(entities.getNumber());
        return response;
    }
}
