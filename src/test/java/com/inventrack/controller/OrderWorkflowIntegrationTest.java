package com.inventrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventrack.dto.request.CreateOrderRequest;
import com.inventrack.dto.request.OrderItemRequest;
import com.inventrack.dto.request.UpdateOrderStatusRequest;
import com.inventrack.entity.OrderStatus;
import com.inventrack.entity.Product;
import com.inventrack.entity.Role;
import com.inventrack.entity.User;
import com.inventrack.repository.ProductRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String staffToken;
    private String adminToken;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        User staff = userRepository.findByUsername("order_staff").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("order_staff")
                        .password("pass")
                        .role(Role.STAFF)
                        .build()));

        User admin = userRepository.findByUsername("admin").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("admin")
                        .password("pass")
                        .role(Role.ADMIN)
                        .build()));

        staffToken = "Bearer " + jwtTokenProvider.generateToken(staff);
        adminToken = "Bearer " + jwtTokenProvider.generateToken(admin);

        testProduct = productRepository.save(Product.builder()
                .name("Order Test Product")
                .category("Test")
                .price(new BigDecimal("25.00"))
                .quantity(10)
                .lowStockThreshold(3)
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("Complete order lifecycle: creation, automatic stock deduction, price snapshotting, and status progression")
    void completeOrderLifecycle() throws Exception {
        // 1. Staff creates order for 4 units of testProduct (4 * 25.00 = 100.00)
        CreateOrderRequest orderRequest = new CreateOrderRequest(List.of(
                new OrderItemRequest(testProduct.getId(), 4)
        ));

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].price").value(25.00))
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andReturn();

        Number orderId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        // 2. Verify product quantity was automatically deducted from 10 to 6
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getQuantity()).isEqualTo(6);

        // 3. Admin confirms and progresses order
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.CONFIRMED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 4. Attempting to order more than remaining stock (e.g. 10 units when only 6 available) should fail
        CreateOrderRequest excessiveOrder = new CreateOrderRequest(List.of(
                new OrderItemRequest(testProduct.getId(), 10)
        ));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveOrder)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Stock"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
