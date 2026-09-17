package com.shopwavefusion.rework.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopwavefusion.rework.domain.AddressEntity;

public interface AddressRepository extends JpaRepository<AddressEntity, UUID> {
    List<AddressEntity> findTop20ByUserIdOrderByIdDesc(UUID userId);
    long countByUserId(UUID userId);
}
