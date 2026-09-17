package com.shopwavefusion.rework.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_number", columnNames = "order_number"),
        @UniqueConstraint(name = "uk_order_user_idempotency", columnNames = { "user_id", "idempotency_key" })
})
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String number;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "ship_first_name", nullable = false, length = 80)
    private String shippingFirstName;
    @Column(name = "ship_last_name", nullable = false, length = 80)
    private String shippingLastName;
    @Column(name = "ship_street", nullable = false, length = 200)
    private String shippingStreet;
    @Column(name = "ship_city", nullable = false, length = 80)
    private String shippingCity;
    @Column(name = "ship_department", nullable = false, length = 40)
    private String shippingDepartment;
    @Column(name = "ship_postal_code", length = 20)
    private String shippingPostalCode;
    @Column(name = "ship_mobile", nullable = false, length = 20)
    private String shippingMobile;
    @Column(name = "ship_country", nullable = false, length = 2)
    private String shippingCountry;

    @Column(name = "subtotal_minor", nullable = false)
    private long subtotalMinor;
    @Column(name = "discount_minor", nullable = false)
    private long discountMinor;
    @Column(name = "total_minor", nullable = false)
    private long totalMinor;
    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;
    @Column(nullable = false, length = 3)
    private String currency = "BOB";
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 16)
    private PaymentMethod paymentMethod = PaymentMethod.MOCK;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 16)
    private PaymentStatus paymentStatus = PaymentStatus.SIMULATED;
    @Column(name = "payment_reference", nullable = false, length = 80)
    private String paymentReference;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();
}
