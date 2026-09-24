package com.inventrack.service;

import com.inventrack.dto.request.CreateOrderRequest;
import com.inventrack.dto.request.OrderItemRequest;
import com.inventrack.dto.request.UpdateOrderStatusRequest;
import com.inventrack.dto.response.OrderResponse;
import com.inventrack.entity.*;
import com.inventrack.exception.InsufficientStockException;
import com.inventrack.exception.InvalidOrderStatusTransitionException;
import com.inventrack.repository.OrderRepository;
import com.inventrack.repository.ProductRepository;
import com.inventrack.repository.UserRepository;
import com.inventrack.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("test_user")
                .role(Role.STAFF)
                .build();

        product1 = Product.builder()
                .id(10L)
                .name("Product A")
                .price(new BigDecimal("50.00"))
                .quantity(20)
                .lowStockThreshold(5)
                .isActive(true)
                .build();

        product2 = Product.builder()
                .id(20L)
                .name("Product B")
                .price(new BigDecimal("30.00"))
                .quantity(15)
                .lowStockThreshold(5)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should create order, calculate total on backend, snapshot prices, and deduct stock")
    void createOrder_Success() {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(10L, 2), // 2 * 50 = 100
                new OrderItemRequest(20L, 3)  // 3 * 30 = 90 -> total = 190.00
        ));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findByIdWithPessimisticLock(10L)).thenReturn(Optional.of(product1));
        when(productRepository.findByIdWithPessimisticLock(20L)).thenReturn(Optional.of(product2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });

        OrderResponse response = orderService.createOrder(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("190.00"));
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getItems()).hasSize(2);

        // Verify product quantities were deducted
        assertThat(product1.getQuantity()).isEqualTo(18); // 20 - 2
        assertThat(product2.getQuantity()).isEqualTo(12); // 15 - 3
        verify(productRepository).save(product1);
        verify(productRepository).save(product2);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when requested quantity exceeds available stock")
    void createOrder_InsufficientStock_ThrowsException() {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(10L, 25) // requested 25, available 20
        ));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findByIdWithPessimisticLock(10L)).thenReturn(Optional.of(product1));

        assertThatThrownBy(() -> orderService.createOrder(request, 1L))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("exceeds available stock");

        // Verify no order is saved and product quantity is unmodified
        assertThat(product1.getQuantity()).isEqualTo(20);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully update order status for valid transition sequence")
    void updateOrderStatus_ValidTransitions() {
        Order order = Order.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // PENDING -> CONFIRMED
        OrderResponse response = orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED));
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // CONFIRMED -> PROCESSING
        response = orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.PROCESSING));
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        // PROCESSING -> SHIPPED
        response = orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.SHIPPED));
        assertThat(response.getStatus()).isEqualTo(OrderStatus.SHIPPED);

        // SHIPPED -> DELIVERED
        response = orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.DELIVERED));
        assertThat(response.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("Should throw InvalidOrderStatusTransitionException when moving backwards from DELIVERED")
    void updateOrderStatus_DeliveredCannotBeMovedBack() {
        Order order = Order.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("100.00"))
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED)))
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .hasMessageContaining("Delivered orders cannot be moved back to earlier states");
    }

    @Test
    @DisplayName("Should throw InvalidOrderStatusTransitionException when modifying a CANCELLED order")
    void updateOrderStatus_CancelledCannotBeModified() {
        Order order = Order.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.CANCELLED)
                .totalAmount(new BigDecimal("100.00"))
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.PENDING)))
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .hasMessageContaining("Cancelled orders cannot be modified");
    }

    @Test
    @DisplayName("Should replenish inventory stock when order is transitioned to CANCELLED")
    void updateOrderStatus_CancellingOrderReplenishesStock() {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .product(product1) // initial quantity 20
                .quantity(5)
                .price(new BigDecimal("50.00"))
                .build();

        Order order = Order.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("250.00"))
                .items(List.of(item))
                .build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithPessimisticLock(10L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        // Quantity should be replenished: 20 + 5 = 25
        assertThat(product1.getQuantity()).isEqualTo(25);
        verify(productRepository).save(product1);
    }
}
