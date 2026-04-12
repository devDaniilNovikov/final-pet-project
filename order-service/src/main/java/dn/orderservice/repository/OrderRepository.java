package dn.orderservice.repository;

import dn.orderservice.entity.OrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderEntity> findByBuyerId(UUID buyerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderEntity> findById(UUID id);


}