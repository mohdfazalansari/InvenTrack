package com.inventrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventrack.dto.request.CreateProductRequest;
import com.inventrack.dto.request.StockAdjustmentRequest;
import com.inventrack.dto.request.UpdateOrderStatusRequest;
import com.inventrack.entity.OrderStatus;
import com.inventrack.entity.Role;
import com.inventrack.entity.User;
import com.inventrack.repository.UserRepository;
import com.inventrack.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        User admin = userRepository.findByUsername("admin").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("admin")
                        .password("pass")
                        .role(Role.ADMIN)
                        .build()));

        User staff = userRepository.findByUsername("staff_user").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("staff_user")
                        .password("pass")
                        .role(Role.STAFF)
                        .build()));

        adminToken = "Bearer " + jwtTokenProvider.generateToken(admin);
        staffToken = "Bearer " + jwtTokenProvider.generateToken(staff);
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint should return 401 Unauthorized")
    void unauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("STAFF calling POST /api/products should return 403 Forbidden")
    void staffCannotCreateProduct_Returns403() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("New Tablet")
                .category("Electronics")
                .price(new BigDecimal("499.99"))
                .quantity(10)
                .lowStockThreshold(2)
                .build();

        mockMvc.perform(post("/api/products")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("STAFF calling POST /api/inventory/{id}/adjust should return 403 Forbidden")
    void staffCannotAdjustInventory_Returns403() throws Exception {
        StockAdjustmentRequest request = new StockAdjustmentRequest(10, "Restock");

        mockMvc.perform(post("/api/inventory/1/adjust")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("STAFF calling PATCH /api/orders/{id}/status should return 403 Forbidden")
    void staffCannotUpdateOrderStatus_Returns403() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);

        mockMvc.perform(patch("/api/orders/1/status")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("ADMIN can create products and return 201 Created")
    void adminCanCreateProduct_Returns201() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Admin Product")
                .description("Special admin test product")
                .category("Office")
                .price(new BigDecimal("89.99"))
                .quantity(100)
                .lowStockThreshold(20)
                .build();

        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Admin Product"))
                .andExpect(jsonPath("$.quantity").value(100));
    }

    @Test
    @DisplayName("Both STAFF and ADMIN can view products")
    void bothStaffAndAdminCanViewProducts() throws Exception {
        // STAFF
        mockMvc.perform(get("/api/products")
                        .header("Authorization", staffToken))
                .andExpect(status().isOk());

        // ADMIN
        mockMvc.perform(get("/api/products")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }
}
