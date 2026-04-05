package dn.productservice.mapper;

import dn.productservice.dto.image.ProductImageRequest;
import dn.productservice.dto.image.ProductImageResponse;
import dn.productservice.entity.ProductImageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductImageMapper {

    ProductImageResponse toResponse(ProductImageEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductImageEntity toEntity(ProductImageRequest request);
}
