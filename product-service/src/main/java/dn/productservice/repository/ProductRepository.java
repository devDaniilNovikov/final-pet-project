package dn.productservice.repository;

import dn.productservice.entity.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    Page<ProductEntity> findAllBySellerId(UUID sellerId, Pageable pageable);

    Page<ProductEntity> findAllByCategoryId(UUID categoryId, Pageable pageable);

    Optional<ProductEntity> findByIdAndQuantityGreaterThanEqual(UUID productId, int quantity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id IN :ids")
    List<ProductEntity> findAllByIdWithLock(@Param("ids") Collection<UUID> ids);
}
