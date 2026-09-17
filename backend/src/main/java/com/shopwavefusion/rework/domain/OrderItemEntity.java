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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "variant_id", nullable = false)
    private UUID variantId;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;
    @Column(name = "variant_label", nullable = false, length = 30)
    private String variantLabel;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "unit_price_minor", nullable = false)
    private long unitPriceMinor;
    @Column(name = "unit_sale_price_minor", nullable = false)
    private long unitSalePriceMinor;
    @Column(name = "line_total_minor", nullable = false)
    private long lineTotalMinor;
}
