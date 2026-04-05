package dn.productservice.repository;

import dn.productservice.dto.product.ProductResponse;
import dn.productservice.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {


    Optional<ProductEntity> findBySellerId(UUID sellerId);

    Page<ProductEntity> findAllBySellerId(UUID sellerId, Pageable pageable);

    Page<ProductEntity> findAllByCategoryId(UUID categoryId, Pageable pageable);

    Optional<ProductEntity> findByIdAndQuantityGreaterThanEqual(UUID productId,int quantity);
}
