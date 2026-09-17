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
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class AddressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;
    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;
    @Column(name = "street_address", nullable = false, length = 200)
    private String streetAddress;
    @Column(nullable = false, length = 80)
    private String city;
    @Column(nullable = false, length = 40)
    private String department;
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    @Column(nullable = false, length = 20)
    private String mobile;
    @Column(nullable = false, length = 2)
    private String country = "BO";
}
