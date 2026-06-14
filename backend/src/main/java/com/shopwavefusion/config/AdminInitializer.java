package com.shopwavefusion.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.shopwavefusion.modal.User;
import com.shopwavefusion.repository.UserRepository;
import com.shopwavefusion.service.CartService;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CartService cartService;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, CartService cartService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail("admin@example.com") != null) {
            return;
        }

        User adminUser = new User();
        adminUser.setEmail("admin@example.com");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("Admin");
        adminUser.setMobile("1234567890");
        adminUser.setPassword(passwordEncoder.encode("admin"));
        adminUser.setRole("ROLE_ADMIN");
        adminUser.setCreatedAt(LocalDateTime.now());

        userRepository.save(adminUser);
        userRepository.flush();

        User reloadedAdmin = userRepository.findByEmail("admin@example.com");
        if (reloadedAdmin == null) {
            System.err.println("[AdminInitializer] No se pudo recargar el admin despues de guardarlo.");
            return;
        }

        try {
            cartService.createCart(reloadedAdmin);
            System.out.println("[AdminInitializer] Admin y cart creados OK.");
        } catch (Exception e) {
            System.err.println("[AdminInitializer] Error creando cart (no critico, seguimos): " + e.getMessage());
        }
    }
}

