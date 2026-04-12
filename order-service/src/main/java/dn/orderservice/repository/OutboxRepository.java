package dn.orderservice.repository;

public interface OutboxRepository extends org.springframework.data.jpa.repository.JpaRepository<dn.orderservice.entity.OutboxEntity, java.util.UUID> {
}