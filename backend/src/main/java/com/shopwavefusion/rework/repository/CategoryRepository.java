package com.shopwavefusion.rework.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopwavefusion.rework.domain.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    List<CategoryEntity> findAllByOrderByLevelAscNameAsc();
}
