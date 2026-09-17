package com.shopwavefusion.rework.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shopwavefusion.rework.domain.ProductVariantEntity;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {
    Optional<ProductVariantEntity> findByIdAndActiveTrue(UUID id);
    List<ProductVariantEntity> findAllByProductId(UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariantEntity v join fetch v.product where v.id in :ids order by v.id")
    List<ProductVariantEntity> findAllForUpdate(@Param("ids") Collection<UUID> ids);
}
