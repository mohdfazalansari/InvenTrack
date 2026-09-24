package com.inventrack.config;

import com.inventrack.entity.Product;
import com.inventrack.entity.Role;
import com.inventrack.entity.User;
import com.inventrack.repository.ProductRepository;
import com.inventrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-username:admin}")
    private String adminUsername;

    @Value("${app.admin.default-password:adminPassword123!}")
    private String adminPassword;

    public DataInitializer(
            UserRepository userRepository,
            ProductRepository productRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Initialize default administrator if none exists
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            logger.info("Default ADMIN user seeded successfully with username: {}", adminUsername);
        }

        // Seed sample products if product catalog is empty
        if (productRepository.count() == 0) {
            Product p1 = Product.builder()
                    .name("Ergonomic Mechanical Keyboard")
                    .description("RGB backlit mechanical keyboard with tactile switches")
                    .category("Peripherals")
                    .price(new BigDecimal("129.99"))
                    .quantity(50)
                    .lowStockThreshold(10)
                    .isActive(true)
                    .build();

            Product p2 = Product.builder()
                    .name("Ultra-Wide 4K Gaming Monitor")
                    .description("34-inch curved gaming display with 144Hz refresh rate")
                    .category("Monitors")
                    .price(new BigDecimal("599.99"))
                    .quantity(15)
                    .lowStockThreshold(5)
                    .isActive(true)
                    .build();

            Product p3 = Product.builder()
                    .name("Wireless Noise-Cancelling Headphones")
                    .description("Over-ear headphones with 30-hour battery life")
                    .category("Audio")
                    .price(new BigDecimal("199.99"))
                    .quantity(4) // low stock intentionally for testing
                    .lowStockThreshold(10)
                    .isActive(true)
                    .build();

            productRepository.saveAll(List.of(p1, p2, p3));
            logger.info("Sample products seeded successfully.");
        }
    }
}
