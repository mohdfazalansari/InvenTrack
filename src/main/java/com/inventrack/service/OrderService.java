package com.inventrack.service;

import com.inventrack.dto.request.CreateOrderRequest;
import com.inventrack.dto.request.UpdateOrderStatusRequest;
import com.inventrack.dto.response.OrderResponse;
import com.inventrack.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request, Long currentUserId);
    Page<OrderResponse> getOrders(OrderStatus status, Long currentUserId, boolean isAdmin, Pageable pageable);
    OrderResponse getOrderById(Long id, Long currentUserId, boolean isAdmin);
    OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request);
}
