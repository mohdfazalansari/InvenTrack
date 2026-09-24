package com.inventrack.service;

import com.inventrack.dto.request.StockAdjustmentRequest;
import com.inventrack.dto.response.InventoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {
    Page<InventoryResponse> getAllInventory(Pageable pageable);
    Page<InventoryResponse> getLowStockInventory(Pageable pageable);
    InventoryResponse adjustStock(Long productId, StockAdjustmentRequest request);
}
