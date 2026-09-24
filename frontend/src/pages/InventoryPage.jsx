import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { inventoryApi } from '../api/inventoryApi';
import { extractErrorMessage } from '../api/client';
import Alert from '../components/Alert';
import Modal from '../components/Modal';

export default function InventoryPage() {
  const { isAdmin } = useAuth();

  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [onlyLowStock, setOnlyLowStock] = useState(false);

  // Stock Adjustment Modal
  const [adjustModalOpen, setAdjustModalOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [adjustMode, setAdjustMode] = useState('add'); // 'add' | 'remove'
  const [adjustmentAmount, setAdjustmentAmount] = useState('');
  const [adjustmentReason, setAdjustmentReason] = useState('');
  const [adjustSubmitting, setAdjustSubmitting] = useState(false);
  const [adjustError, setAdjustError] = useState('');

  const fetchInventory = async () => {
    setLoading(true);
    setError('');
    try {
      const data = onlyLowStock
        ? await inventoryApi.getLowStock({ size: 100 })
        : await inventoryApi.getInventory({ size: 100 });
      setInventory(data.content || []);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInventory();
  }, [onlyLowStock]);

  const handleOpenAdjust = (item, mode) => {
    setSelectedProduct(item);
    setAdjustMode(mode);
    setAdjustmentAmount('');
    setAdjustmentReason('');
    setAdjustError('');
    setAdjustModalOpen(true);
  };

  const handleAdjustSubmit = async (e) => {
    e.preventDefault();
    setAdjustError('');

    const qty = parseInt(adjustmentAmount, 10);
    if (!qty || qty <= 0) {
      setAdjustError('Please specify a positive quantity to adjust.');
      return;
    }

    const delta = adjustMode === 'add' ? qty : -qty;

    setAdjustSubmitting(true);
    try {
      const updated = await inventoryApi.adjustStock(selectedProduct.productId, {
        quantityDelta: delta,
        reason: adjustmentReason.trim() || (adjustMode === 'add' ? 'Manual Restock' : 'Stock Adjustment'),
      });

      setSuccess(
        `Successfully updated stock for "${updated.productName}". New quantity: ${updated.quantity}`
      );
      setAdjustModalOpen(false);
      fetchInventory();
    } catch (err) {
      setAdjustError(extractErrorMessage(err));
    } finally {
      setAdjustSubmitting(false);
    }
  };

  const getStockStatusBadge = (quantity, threshold) => {
    if (quantity === 0) {
      return <span className="status-badge status-out">OUT OF STOCK</span>;
    }
    if (quantity <= threshold) {
      return <span className="status-badge status-low">LOW STOCK</span>;
    }
    return <span className="status-badge status-in">IN STOCK</span>;
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Inventory Tracking</h1>
          <p className="page-subtitle">Monitor stock quantities and manage stock adjustments</p>
        </div>
        <div className="header-actions">
          <button
            className={`btn ${onlyLowStock ? 'btn-warning' : 'btn-secondary'}`}
            onClick={() => setOnlyLowStock(!onlyLowStock)}
          >
            {onlyLowStock ? 'Show All Products' : '⚠️ Show Low Stock Only'}
          </button>
          <button className="btn btn-secondary" onClick={fetchInventory} disabled={loading}>
            🔄 Refresh
          </button>
        </div>
      </div>

      {success && <Alert type="success" message={success} onClose={() => setSuccess('')} />}
      {error && <Alert type="error" message={error} onClose={() => setError('')} />}

      <div className="card">
        <div className="card-body p-0">
          {loading ? (
            <div className="p-4 text-center text-muted">Loading inventory stock levels...</div>
          ) : inventory.length === 0 ? (
            <div className="p-4 text-center text-muted">No inventory items found.</div>
          ) : (
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Category</th>
                    <th>Current Stock</th>
                    <th>Low Stock Threshold</th>
                    <th>Stock Status</th>
                    <th>Stock Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {inventory.map((item) => (
                    <tr key={item.productId}>
                      <td>
                        <strong>{item.productName}</strong>
                      </td>
                      <td>
                        <span className="category-pill">{item.category}</span>
                      </td>
                      <td>
                        <strong className="stock-number">{item.quantity}</strong> units
                      </td>
                      <td>{item.lowStockThreshold} units</td>
                      <td>{getStockStatusBadge(item.quantity, item.lowStockThreshold)}</td>
                      <td>
                        {isAdmin ? (
                          <div className="stock-action-buttons">
                            <button
                              className="btn btn-sm btn-outline-success"
                              onClick={() => handleOpenAdjust(item, 'add')}
                              title="Add Stock"
                            >
                              + Add
                            </button>
                            <button
                              className="btn btn-sm btn-outline-danger"
                              onClick={() => handleOpenAdjust(item, 'remove')}
                              disabled={item.quantity === 0}
                              title="Remove Stock"
                            >
                              - Remove
                            </button>
                          </div>
                        ) : (
                          <span className="text-muted small">Admin Only</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Adjust Stock Modal */}
      <Modal
        isOpen={adjustModalOpen}
        title={
          adjustMode === 'add'
            ? `Add Stock: ${selectedProduct?.productName}`
            : `Remove Stock: ${selectedProduct?.productName}`
        }
        onClose={() => setAdjustModalOpen(false)}
      >
        {adjustError && <Alert type="error" message={adjustError} onClose={() => setAdjustError('')} />}

        <div className="stock-current-info mb-3">
          <p>
            Current Stock: <strong>{selectedProduct?.quantity} units</strong> (Threshold:{' '}
            {selectedProduct?.lowStockThreshold})
          </p>
        </div>

        <form onSubmit={handleAdjustSubmit}>
          <div className="form-group">
            <label htmlFor="adjust-qty">
              {adjustMode === 'add' ? 'Quantity to Add *' : 'Quantity to Deduct *'}
            </label>
            <input
              id="adjust-qty"
              type="number"
              min="1"
              max={adjustMode === 'remove' ? selectedProduct?.quantity : undefined}
              className="form-control"
              placeholder="e.g. 10"
              value={adjustmentAmount}
              onChange={(e) => setAdjustmentAmount(e.target.value)}
              required
              autoFocus
            />
            {adjustMode === 'remove' && (
              <small className="form-text text-muted">
                Quantity cannot exceed current available stock ({selectedProduct?.quantity}).
              </small>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="adjust-reason">Reason / Reference Note</label>
            <input
              id="adjust-reason"
              type="text"
              className="form-control"
              placeholder="e.g. Supplier delivery PO-9842 or Damaged goods"
              value={adjustmentReason}
              onChange={(e) => setAdjustmentReason(e.target.value)}
            />
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setAdjustModalOpen(false)}
              disabled={adjustSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className={`btn ${adjustMode === 'add' ? 'btn-success' : 'btn-danger'}`}
              disabled={adjustSubmitting}
            >
              {adjustSubmitting
                ? 'Updating Stock...'
                : adjustMode === 'add'
                ? 'Confirm Stock Addition'
                : 'Confirm Stock Deduction'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
