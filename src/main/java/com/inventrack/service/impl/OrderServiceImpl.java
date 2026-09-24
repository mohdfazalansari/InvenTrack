package com.inventrack.service.impl;

import com.inventrack.dto.request.CreateOrderRequest;
import com.inventrack.dto.request.OrderItemRequest;
import com.inventrack.dto.request.UpdateOrderStatusRequest;
import com.inventrack.dto.response.OrderItemResponse;
import com.inventrack.dto.response.OrderResponse;
import com.inventrack.entity.*;
import com.inventrack.exception.InsufficientStockException;
import com.inventrack.exception.InvalidOrderStatusTransitionException;
import com.inventrack.exception.ResourceNotFoundException;
import com.inventrack.repository.OrderRepository;
import com.inventrack.repository.ProductRepository;
import com.inventrack.repository.UserRepository;
import com.inventrack.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        BigDecimal calculatedTotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        for (OrderItemRequest itemReq : request.getItems()) {
            // Lock product row to prevent concurrent race conditions
            Product product = productRepository.findByIdWithPessimisticLock(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

            if (Boolean.FALSE.equals(product.getIsActive())) {
                throw new ResourceNotFoundException("Product is no longer active with id: " + itemReq.getProductId());
            }

            if (product.getQuantity() < itemReq.getQuantity()) {
                throw new InsufficientStockException(String.format(
                        "Requested quantity (%d) exceeds available stock (%d) for product '%s'.",
                        itemReq.getQuantity(), product.getQuantity(), product.getName()));
            }

            // Deduct inventory atomically
            product.setQuantity(product.getQuantity() - itemReq.getQuantity());
            productRepository.save(product);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            calculatedTotal = calculatedTotal.add(lineTotal);

            // Snapshot product price at the time of purchase
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .price(product.getPrice())
                    .build();

            order.addItem(orderItem);
        }

        order.setTotalAmount(calculatedTotal);
        Order savedOrder = orderRepository.save(order);

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(OrderStatus status, Long currentUserId, boolean isAdmin, Pageable pageable) {
        Page<Order> orders;
        if (isAdmin) {
            if (status != null) {
                orders = orderRepository.findByStatus(status, pageable);
            } else {
                orders = orderRepository.findAll(pageable);
            }
        } else {
            // STAFF can view all orders or their own orders (we support both; defaults to all orders if both ADMIN and STAFF have view order permissions)
            if (status != null) {
                orders = orderRepository.findByStatus(status, pageable);
            } else {
                orders = orderRepository.findAll(pageable);
            }
        }
        return orders.map(this::mapToOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, Long currentUserId, boolean isAdmin) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = request.getStatus();

        if (currentStatus == targetStatus) {
            return mapToOrderResponse(order);
        }

        validateStatusTransition(currentStatus, targetStatus);

        // If transitioning to CANCELLED, restore inventory to products
        if (targetStatus == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product product = productRepository.findByIdWithPessimisticLock(item.getProduct().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProduct().getId()));

                product.setQuantity(product.getQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus(targetStatus);
        Order updatedOrder = orderRepository.save(order);

        return mapToOrderResponse(updatedOrder);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        if (current == OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusTransitionException("Delivered orders cannot be moved back to earlier states.");
        }

        if (current == OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusTransitionException("Cancelled orders cannot be modified.");
        }

        boolean isValid = switch (current) {
            case PENDING -> target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case CONFIRMED -> target == OrderStatus.PROCESSING || target == OrderStatus.CANCELLED;
            case PROCESSING -> target == OrderStatus.SHIPPED || target == OrderStatus.CANCELLED;
            case SHIPPED -> target == OrderStatus.DELIVERED;
            default -> false;
        };

        if (!isValid) {
            throw new InvalidOrderStatusTransitionException(String.format(
                    "Invalid order status transition from %s to %s.", current, target));
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                itemResponses.add(OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subtotal(subtotal)
                        .build());
            }
        }

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .username(order.getUser().getUsername())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
