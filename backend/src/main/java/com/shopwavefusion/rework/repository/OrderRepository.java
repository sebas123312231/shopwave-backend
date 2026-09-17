package com.shopwavefusion.rework.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import com.shopwavefusion.rework.domain.OrderEntity;
import com.shopwavefusion.rework.domain.OrderStatus;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Page<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<OrderEntity> findByIdAndUserId(UUID id, UUID userId);
    Optional<OrderEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    Page<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct o from OrderEntity o left join fetch o.items where o.id = :id")
    Optional<OrderEntity> findForUpdate(@Param("id") UUID id);
}
