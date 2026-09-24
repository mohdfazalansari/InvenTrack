package com.inventrack.service.impl;

import com.inventrack.dto.request.StockAdjustmentRequest;
import com.inventrack.dto.response.InventoryResponse;
import com.inventrack.entity.Product;
import com.inventrack.exception.InsufficientStockException;
import com.inventrack.exception.ResourceNotFoundException;
import com.inventrack.repository.ProductRepository;
import com.inventrack.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;

    public InventoryServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAllInventory(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable)
                .map(this::mapToInventoryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getLowStockInventory(Pageable pageable) {
        return productRepository.findLowStockProducts(pageable)
                .map(this::mapToInventoryResponse);
    }

    @Override
    @Transactional
    public InventoryResponse adjustStock(Long productId, StockAdjustmentRequest request) {
        // Fetch with pessimistic lock to prevent race condition during stock adjustment
        Product product = productRepository.findByIdWithPessimisticLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        int newQuantity = product.getQuantity() + request.getQuantityDelta();
        if (newQuantity < 0) {
            throw new InsufficientStockException(String.format(
                    "Stock adjustment would result in negative quantity. Current stock: %d, adjustment: %d",
                    product.getQuantity(), request.getQuantityDelta()));
        }

        product.setQuantity(newQuantity);
        Product savedProduct = productRepository.save(product);

        return mapToInventoryResponse(savedProduct);
    }

    private InventoryResponse mapToInventoryResponse(Product product) {
        return InventoryResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .category(product.getCategory())
                .quantity(product.getQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .lowStock(product.isLowStock())
                .build();
    }
}
