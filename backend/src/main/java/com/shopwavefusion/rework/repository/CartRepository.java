package com.shopwavefusion.rework.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shopwavefusion.rework.domain.CartEntity;

public interface CartRepository extends JpaRepository<CartEntity, UUID> {
    Optional<CartEntity> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct c from CartEntity c left join fetch c.items i left join fetch i.variant v left join fetch v.product where c.user.id = :userId")
    Optional<CartEntity> findByUserIdForUpdate(@Param("userId") UUID userId);
}
