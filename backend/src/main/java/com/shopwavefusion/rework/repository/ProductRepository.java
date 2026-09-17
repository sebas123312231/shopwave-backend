package com.shopwavefusion.rework.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.shopwavefusion.rework.domain.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {
    Page<ProductEntity> findAllByActiveTrue(Pageable pageable);
    long countByActiveTrue();
    Optional<ProductEntity> findByIdAndActiveTrue(UUID id);
}
