import React, { useState, useEffect } from 'react';
import { productApi } from '../api/productApi';
import { orderApi } from '../api/orderApi';
import { extractErrorMessage } from '../api/client';
import Alert from '../components/Alert';

export default function CreateOrderPage({ setCurrentView }) {
  const [catalogProducts, setCatalogProducts] = useState([]);
  const [loadingCatalog, setLoadingCatalog] = useState(true);
  const [catalogError, setCatalogError] = useState('');

  // Order items state: [{ productId, quantity, productObj }]
  const [selectedItems, setSelectedItems] = useState([]);
  const [selectedProductId, setSelectedProductId] = useState('');
  const [inputQuantity, setInputQuantity] = useState(1);

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [submitSuccess, setSubmitSuccess] = useState('');

  useEffect(() => {
    const fetchCatalog = async () => {
      setLoadingCatalog(true);
      try {
        const data = await productApi.getProducts({ size: 100 });
        setCatalogProducts(data.content || []);
      } catch (err) {
        setCatalogError(extractErrorMessage(err));
      } finally {
        setLoadingCatalog(false);
      }
    };
    fetchCatalog();
  }, []);

  const handleAddItem = (e) => {
    e.preventDefault();
    setSubmitError('');

    if (!selectedProductId) {
      setSubmitError('Please select a product to add.');
      return;
    }

    const prodId = Number(selectedProductId);
    const product = catalogProducts.find((p) => p.id === prodId);
    if (!product) return;

    const qty = parseInt(inputQuantity, 10);
    if (!qty || qty <= 0) {
      setSubmitError('Quantity must be at least 1.');
      return;
    }

    // Check if already in order
    const existingIndex = selectedItems.findIndex((item) => item.productId === prodId);
    if (existingIndex > -1) {
      const existing = selectedItems[existingIndex];
      const newTotalQty = existing.quantity + qty;
      const updated = [...selectedItems];
      updated[existingIndex] = {
        ...existing,
        quantity: newTotalQty,
      };
      setSelectedItems(updated);
    } else {
      setSelectedItems([
        ...selectedItems,
        {
          productId: prodId,
          productName: product.name,
          category: product.category,
          unitPrice: product.price,
          availableStock: product.quantity,
          quantity: qty,
        },
      ]);
    }

    setSelectedProductId('');
    setInputQuantity(1);
  };

  const handleRemoveItem = (index) => {
    const updated = selectedItems.filter((_, i) => i !== index);
    setSelectedItems(updated);
  };

  const handleUpdateItemQty = (index, newQty) => {
    const qty = parseInt(newQty, 10);
    if (isNaN(qty) || qty <= 0) return;
    const updated = [...selectedItems];
    updated[index].quantity = qty;
    setSelectedItems(updated);
  };

  // Frontend estimated total calculation (strictly for user convenience)
  const estimatedTotal = selectedItems.reduce((acc, item) => {
    return acc + Number(item.unitPrice) * item.quantity;
  }, 0);

  const handleSubmitOrder = async () => {
    if (selectedItems.length === 0) {
      setSubmitError('Please add at least one product line item to place an order.');
      return;
    }

    setSubmitting(true);
    setSubmitError('');
    try {
      const payload = selectedItems.map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
      }));

      const createdOrder = await orderApi.createOrder(payload);
      setSubmitSuccess(
        `Order #${createdOrder.id} placed successfully! Authoritative Total: $${Number(
          createdOrder.totalAmount
        ).toFixed(2)}. Redirecting to orders...`
      );

      setSelectedItems([]);
      setTimeout(() => {
        setCurrentView('orders');
      }, 1500);
    } catch (err) {
      setSubmitError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const selectedProductObj = catalogProducts.find((p) => p.id === Number(selectedProductId));

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Create New Order</h1>
          <p className="page-subtitle">
            Configure line items from inventory catalog and place a new customer order
          </p>
        </div>
        <div className="header-actions">
          <button className="btn btn-secondary" onClick={() => setCurrentView('orders')}>
            ← Back to Orders
          </button>
        </div>
      </div>

      {catalogError && <Alert type="error" message={catalogError} />}
      {submitError && <Alert type="error" message={submitError} onClose={() => setSubmitError('')} />}
      {submitSuccess && <Alert type="success" message={submitSuccess} />}

      <div className="create-order-layout">
        {/* Left Column: Product Selection Form */}
        <div className="card">
          <div className="card-header">
            <h3>Add Items to Order</h3>
          </div>
          <div className="card-body">
            {loadingCatalog ? (
              <p className="text-muted">Loading available products...</p>
            ) : catalogProducts.length === 0 ? (
              <p className="text-muted">No products available in catalog.</p>
            ) : (
              <form onSubmit={handleAddItem}>
                <div className="form-group">
                  <label htmlFor="select-product">Select Product *</label>
                  <select
                    id="select-product"
                    className="form-control"
                    value={selectedProductId}
                    onChange={(e) => setSelectedProductId(e.target.value)}
                  >
                    <option value="">-- Choose a product --</option>
                    {catalogProducts.map((p) => (
                      <option key={p.id} value={p.id} disabled={p.quantity === 0}>
                        {p.name} (${Number(p.price).toFixed(2)}) - Available: {p.quantity} units
                        {p.quantity === 0 ? ' [OUT OF STOCK]' : ''}
                      </option>
                    ))}
                  </select>
                </div>

                {selectedProductObj && (
                  <div className="product-selection-preview mb-3 p-2 bg-light rounded">
                    <div>Price: <strong>${Number(selectedProductObj.price).toFixed(2)}</strong></div>
                    <div>
                      Stock Available:{' '}
                      <strong className={selectedProductObj.quantity <= selectedProductObj.lowStockThreshold ? 'text-warning' : 'text-success'}>
                        {selectedProductObj.quantity} units
                      </strong>
                    </div>
                  </div>
                )}

                <div className="form-group">
                  <label htmlFor="input-qty">Quantity *</label>
                  <input
                    id="input-qty"
                    type="number"
                    min="1"
                    className="form-control"
                    value={inputQuantity}
                    onChange={(e) => setInputQuantity(e.target.value)}
                    required
                  />
                </div>

                <button
                  type="submit"
                  className="btn btn-secondary btn-block"
                  disabled={!selectedProductId || (selectedProductObj && selectedProductObj.quantity === 0)}
                >
                  ➕ Add Line Item
                </button>
              </form>
            )}
          </div>
        </div>

        {/* Right Column: Order Summary / Cart */}
        <div className="card">
          <div className="card-header flex-between">
            <h3>Order Summary ({selectedItems.length} items)</h3>
            {selectedItems.length > 0 && (
              <button className="btn-sm btn-link text-danger" onClick={() => setSelectedItems([])}>
                Clear All
              </button>
            )}
          </div>
          <div className="card-body p-0">
            {selectedItems.length === 0 ? (
              <div className="p-4 text-center text-muted">
                No items added yet. Choose a product from the catalog on the left to begin building the order.
              </div>
            ) : (
              <div className="table-responsive">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Product</th>
                      <th>Unit Price</th>
                      <th style={{ width: '100px' }}>Qty</th>
                      <th>Subtotal</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedItems.map((item, idx) => (
                      <tr key={item.productId}>
                        <td>
                          <strong>{item.productName}</strong>
                          <div className="small text-muted">Stock: {item.availableStock}</div>
                        </td>
                        <td>${Number(item.unitPrice).toFixed(2)}</td>
                        <td>
                          <input
                            type="number"
                            min="1"
                            className="form-control-sm"
                            value={item.quantity}
                            onChange={(e) => handleUpdateItemQty(idx, e.target.value)}
                          />
                        </td>
                        <td>
                          <strong>${(Number(item.unitPrice) * item.quantity).toFixed(2)}</strong>
                        </td>
                        <td>
                          <button
                            type="button"
                            className="btn-icon btn-delete"
                            title="Remove item"
                            onClick={() => handleRemoveItem(idx)}
                          >
                            ✕
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {selectedItems.length > 0 && (
            <div className="card-footer">
              <div className="order-totals-box mb-3">
                <div className="flex-between">
                  <span>Estimated Total (Frontend):</span>
                  <span className="h4 text-primary">${estimatedTotal.toFixed(2)}</span>
                </div>
                <div className="small text-muted mt-1">
                  ℹ️ The backend validates available inventory and performs the final authoritative total calculation inside an atomic transaction.
                </div>
              </div>

              <button
                type="button"
                className="btn btn-primary btn-block btn-lg"
                onClick={handleSubmitOrder}
                disabled={submitting}
              >
                {submitting ? 'Validating Stock & Placing Order...' : '🚀 Submit Order'}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
