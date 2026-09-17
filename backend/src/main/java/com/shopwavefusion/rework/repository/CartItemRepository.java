package com.shopwavefusion.rework.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopwavefusion.rework.domain.CartItemEntity;

public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {
    Optional<CartItemEntity> findByIdAndCartUserId(UUID id, UUID userId);
    boolean existsByVariantId(UUID variantId);
}
