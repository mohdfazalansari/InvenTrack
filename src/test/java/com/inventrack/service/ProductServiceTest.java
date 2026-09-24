package com.inventrack.service;

import com.inventrack.dto.request.CreateProductRequest;
import com.inventrack.dto.request.UpdateProductRequest;
import com.inventrack.dto.response.ProductResponse;
import com.inventrack.entity.Product;
import com.inventrack.exception.ResourceNotFoundException;
import com.inventrack.repository.ProductRepository;
import com.inventrack.service.impl.ProductServiceImpl;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Wireless Mouse")
                .description("Ergonomic mouse")
                .category("Peripherals")
                .price(new BigDecimal("29.99"))
                .quantity(50)
                .lowStockThreshold(10)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should create product successfully")
    void createProduct_Success() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Wireless Mouse")
                .description("Ergonomic mouse")
                .category("Peripherals")
                .price(new BigDecimal("29.99"))
                .quantity(50)
                .lowStockThreshold(10)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Wireless Mouse");
        assertThat(response.getQuantity()).isEqualTo(50);
        assertThat(response.isLowStock()).isFalse();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should update product successfully")
    void updateProduct_Success() {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Wireless Mouse Pro")
                .description("Updated ergonomic mouse")
                .category("Peripherals")
                .price(new BigDecimal("39.99"))
                .quantity(40)
                .lowStockThreshold(10)
                .build();

        when(productRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.updateProduct(1L, request);

        assertThat(response).isNotNull();
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent product")
    void updateProduct_NotFound_ThrowsException() {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Mouse")
                .category("Peripherals")
                .price(BigDecimal.TEN)
                .quantity(5)
                .lowStockThreshold(2)
                .build();

        when(productRepository.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("Should soft delete product successfully")
    void deleteProduct_SoftDeletes() {
        when(productRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        assertThat(product.getIsActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should fetch all products with pagination")
    void getAllProducts_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product));

        when(productRepository.findByIsActiveTrue(pageRequest)).thenReturn(page);

        Page<ProductResponse> responses = productService.getAllProducts(null, pageRequest);

        assertThat(responses).hasSize(1);
        assertThat(responses.getContent().get(0).getName()).isEqualTo("Wireless Mouse");
    }
}
