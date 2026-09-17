package com.shopwavefusion.rework.config;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shopwavefusion.rework.domain.CartEntity;
import com.shopwavefusion.rework.domain.Role;
import com.shopwavefusion.rework.domain.UserEntity;
import com.shopwavefusion.rework.repository.CartRepository;
import com.shopwavefusion.rework.repository.UserRepository;

@Component
@ConditionalOnProperty(name = "shopwave.demo-admin.enabled", havingValue = "true")
public class DemoAdminInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final CartRepository carts;
    private final PasswordEncoder encoder;
    private final String password;

    public DemoAdminInitializer(UserRepository users, CartRepository carts, PasswordEncoder encoder,
                                @Value("${shopwave.demo-admin.password}") String password) {
        this.users = users; this.carts = carts; this.encoder = encoder; this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (password == null || password.length() < 12) throw new IllegalStateException("SHOPWAVE_DEMO_ADMIN_PASSWORD must be supplied when demo admin is enabled");
        UserEntity user = users.findByEmail("admin@shopwave.local").orElseGet(() -> {
            UserEntity created = new UserEntity(); created.setEmail("admin@shopwave.local"); created.setFirstName("ShopWave"); created.setLastName("Admin"); created.setMobile("70000000"); created.setRole(Role.ADMIN); created.setPasswordHash(encoder.encode(password)); created.setCreatedAt(Instant.now()); created.setUpdatedAt(Instant.now()); return users.save(created);
        });
        if (carts.findByUserId(user.getId()).isEmpty()) { CartEntity cart = new CartEntity(); cart.setUser(user); carts.save(cart); }
    }
}
