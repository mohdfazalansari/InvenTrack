package com.inventrack.service;

import com.inventrack.dto.request.CreateProductRequest;
import com.inventrack.dto.request.UpdateProductRequest;
import com.inventrack.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request);
    ProductResponse updateProduct(Long id, UpdateProductRequest request);
    void deleteProduct(Long id);
    ProductResponse getProductById(Long id);
    Page<ProductResponse> getAllProducts(String category, Pageable pageable);
}
