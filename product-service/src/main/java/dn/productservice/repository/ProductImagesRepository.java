package dn.productservice.repository;

import dn.productservice.entity.ProductImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductImagesRepository extends JpaRepository<ProductImageEntity, UUID> {
}
