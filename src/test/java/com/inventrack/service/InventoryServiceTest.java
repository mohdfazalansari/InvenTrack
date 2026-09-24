package com.inventrack.service;

import com.inventrack.dto.request.StockAdjustmentRequest;
import com.inventrack.dto.response.InventoryResponse;
import com.inventrack.entity.Product;
import com.inventrack.exception.InsufficientStockException;
import com.inventrack.repository.ProductRepository;
import com.inventrack.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Mechanical Keyboard")
                .category("Peripherals")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .lowStockThreshold(5)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should increase product inventory when positive delta is provided")
    void adjustStock_Increase_Success() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(15, "Restocking batch");

        when(productRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        InventoryResponse response = inventoryService.adjustStock(1L, request);

        assertThat(response).isNotNull();
        assertThat(product.getQuantity()).isEqualTo(25);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should decrease product inventory when valid negative delta is provided")
    void adjustStock_Decrease_Success() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(-5, "Damaged stock removal");

        when(productRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        InventoryResponse response = inventoryService.adjustStock(1L, request);

        assertThat(response).isNotNull();
        assertThat(product.getQuantity()).isEqualTo(5);
        assertThat(product.isLowStock()).isTrue(); // 5 <= 5
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when stock adjustment results in negative quantity")
    void adjustStock_NegativeResult_ThrowsException() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(-15, "Invalid removal");

        when(productRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> inventoryService.adjustStock(1L, request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("negative quantity");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve only products meeting low stock criteria")
    void getLowStockInventory_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        product.setQuantity(3); // below threshold of 5
        Page<Product> lowStockPage = new PageImpl<>(List.of(product));

        when(productRepository.findLowStockProducts(pageRequest)).thenReturn(lowStockPage);

        Page<InventoryResponse> responses = inventoryService.getLowStockInventory(pageRequest);

        assertThat(responses).hasSize(1);
        assertThat(responses.getContent().get(0).getQuantity()).isEqualTo(3);
        assertThat(responses.getContent().get(0).isLowStock()).isTrue();
    }
}
