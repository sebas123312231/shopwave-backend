package com.shopwavefusion.rework.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_variants", uniqueConstraints = @UniqueConstraint(name = "uk_variant_product_label", columnNames = {"product_id", "label_normalized"}))
@Getter
@Setter
@NoArgsConstructor
public class ProductVariantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;
    @Column(nullable = false, length = 30)
    private String label;
    @Column(name = "label_normalized", nullable = false, length = 30)
    private String labelNormalized;
    @Column(nullable = false)
    private int stock;
    @Column(nullable = false)
    private boolean active = true;
    @Version
    @Column(nullable = false)
    private long version;
}
