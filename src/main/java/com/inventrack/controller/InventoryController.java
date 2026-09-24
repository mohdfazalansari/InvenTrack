package com.inventrack.controller;

import com.inventrack.dto.request.StockAdjustmentRequest;
import com.inventrack.dto.response.InventoryResponse;
import com.inventrack.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Endpoints for inventory tracking and stock adjustments")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "View inventory", description = "Retrieves current stock levels for all products (ADMIN or STAFF).")
    public ResponseEntity<Page<InventoryResponse>> getInventory(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<InventoryResponse> response = inventoryService.getAllInventory(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "View low-stock products", description = "Retrieves products where quantity <= lowStockThreshold (ADMIN or STAFF).")
    public ResponseEntity<Page<InventoryResponse>> getLowStockInventory(
            @PageableDefault(size = 10, sort = "quantity", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<InventoryResponse> response = inventoryService.getLowStockInventory(pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adjust inventory stock", description = "Adjusts product stock by adding or subtracting quantity (ADMIN only).")
    public ResponseEntity<InventoryResponse> adjustStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request) {
        InventoryResponse response = inventoryService.adjustStock(productId, request);
        return ResponseEntity.ok(response);
    }
}
