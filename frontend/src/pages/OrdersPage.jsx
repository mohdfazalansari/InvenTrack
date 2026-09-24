import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { orderApi } from '../api/orderApi';
import { extractErrorMessage } from '../api/client';
import Alert from '../components/Alert';
import Modal from '../components/Modal';

export default function OrdersPage({ setCurrentView }) {
  const { isAdmin } = useAuth();

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // Order Details Modal
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);

  // Status Update State
  const [statusUpdating, setStatusUpdating] = useState(false);
  const [newStatus, setNewStatus] = useState('');

  const fetchOrders = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await orderApi.getOrders({ status: statusFilter, size: 100 });
      setOrders(data.content || []);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, [statusFilter]);

  const handleOpenDetails = (order) => {
    setSelectedOrder(order);
    setNewStatus(order.status);
    setDetailModalOpen(true);
  };

  const handleStatusChange = async () => {
    if (!selectedOrder || newStatus === selectedOrder.status) return;

    setStatusUpdating(true);
    try {
      const updated = await orderApi.updateOrderStatus(selectedOrder.id, newStatus);
      setSelectedOrder(updated);
      setSuccess(`Order #${updated.id} status updated to ${updated.status}.`);
      fetchOrders();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setStatusUpdating(false);
    }
  };

  const getAvailableNextStatuses = (current) => {
    switch (current) {
      case 'PENDING':
        return ['CONFIRMED', 'CANCELLED'];
      case 'CONFIRMED':
        return ['PROCESSING', 'CANCELLED'];
      case 'PROCESSING':
        return ['SHIPPED', 'CANCELLED'];
      case 'SHIPPED':
        return ['DELIVERED'];
      default:
        return [];
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Order Management</h1>
          <p className="page-subtitle">Track customer orders and manage fulfillment statuses</p>
        </div>
        <div className="header-actions">
          <button className="btn btn-primary" onClick={() => setCurrentView('create-order')}>
            ➕ Create New Order
          </button>
          <button className="btn btn-secondary" onClick={fetchOrders} disabled={loading}>
            🔄 Refresh
          </button>
        </div>
      </div>

      {success && <Alert type="success" message={success} onClose={() => setSuccess('')} />}
      {error && <Alert type="error" message={error} onClose={() => setError('')} />}

      <div className="card">
        <div className="card-header flex-between">
          <div className="filter-group">
            <label htmlFor="status-filter">Filter by Status:</label>
            <select
              id="status-filter"
              className="form-control-sm"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="PENDING">PENDING</option>
              <option value="CONFIRMED">CONFIRMED</option>
              <option value="PROCESSING">PROCESSING</option>
              <option value="SHIPPED">SHIPPED</option>
              <option value="DELIVERED">DELIVERED</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </div>
          <span className="text-muted small">Showing {orders.length} orders</span>
        </div>

        <div className="card-body p-0">
          {loading ? (
            <div className="p-4 text-center text-muted">Loading orders...</div>
          ) : orders.length === 0 ? (
            <div className="p-4 text-center text-muted">
              No orders found matching the criteria.
            </div>
          ) : (
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Customer / User</th>
                    <th>Items</th>
                    <th>Total Amount</th>
                    <th>Status</th>
                    <th>Created At</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((o) => (
                    <tr key={o.id}>
                      <td>
                        <strong>#{o.id}</strong>
                      </td>
                      <td>{o.username}</td>
                      <td>{o.items?.length || 0} line item(s)</td>
                      <td>
                        <strong>${Number(o.totalAmount).toFixed(2)}</strong>
                      </td>
                      <td>
                        <span className={`badge badge-status-${o.status.toLowerCase()}`}>
                          {o.status}
                        </span>
                      </td>
                      <td>
                        {o.createdAt ? new Date(o.createdAt).toLocaleString() : 'N/A'}
                      </td>
                      <td>
                        <button
                          className="btn btn-sm btn-outline-primary"
                          onClick={() => handleOpenDetails(o)}
                        >
                          👁️ View Details
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Order Details & Status Update Modal */}
      <Modal
        isOpen={detailModalOpen}
        title={`Order Details: #${selectedOrder?.id}`}
        onClose={() => setDetailModalOpen(false)}
      >
        {selectedOrder && (
          <div className="order-details-container">
            <div className="order-meta-grid mb-3">
              <div>
                <span className="text-muted small">Order ID:</span>
                <div><strong>#{selectedOrder.id}</strong></div>
              </div>
              <div>
                <span className="text-muted small">Placed By:</span>
                <div>{selectedOrder.username}</div>
              </div>
              <div>
                <span className="text-muted small">Status:</span>
                <div>
                  <span className={`badge badge-status-${selectedOrder.status.toLowerCase()}`}>
                    {selectedOrder.status}
                  </span>
                </div>
              </div>
              <div>
                <span className="text-muted small">Date Placed:</span>
                <div>{selectedOrder.createdAt ? new Date(selectedOrder.createdAt).toLocaleString() : 'N/A'}</div>
              </div>
            </div>

            <h4 className="mb-2">Line Items (Snapshot Purchase Prices)</h4>
            <div className="table-responsive mb-3">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Item</th>
                    <th>Unit Price</th>
                    <th>Quantity</th>
                    <th>Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedOrder.items?.map((item) => (
                    <tr key={item.id}>
                      <td>{item.productName}</td>
                      <td>${Number(item.price).toFixed(2)}</td>
                      <td>{item.quantity}</td>
                      <td>${Number(item.subtotal).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan="3" className="text-right">
                      <strong>Authoritative Total:</strong>
                    </td>
                    <td>
                      <strong className="text-primary">
                        ${Number(selectedOrder.totalAmount).toFixed(2)}
                      </strong>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>

            {/* Admin Status Update Section */}
            {isAdmin && (
              <div className="order-status-management card p-3 mt-3 bg-light">
                <h5 className="mb-2">Admin Status Transition</h5>
                {getAvailableNextStatuses(selectedOrder.status).length === 0 ? (
                  <p className="text-muted small mb-0">
                    Order is in terminal state (<strong>{selectedOrder.status}</strong>) and cannot be modified.
                  </p>
                ) : (
                  <div className="status-update-row">
                    <select
                      className="form-control-sm"
                      value={newStatus}
                      onChange={(e) => setNewStatus(e.target.value)}
                    >
                      <option value={selectedOrder.status}>Current: {selectedOrder.status}</option>
                      {getAvailableNextStatuses(selectedOrder.status).map((st) => (
                        <option key={st} value={st}>
                          Advance to: {st}
                        </option>
                      ))}
                    </select>
                    <button
                      className="btn btn-sm btn-primary"
                      onClick={handleStatusChange}
                      disabled={statusUpdating || newStatus === selectedOrder.status}
                    >
                      {statusUpdating ? 'Updating...' : 'Apply Status'}
                    </button>
                  </div>
                )}
              </div>
            )}

            <div className="modal-footer mt-3">
              <button className="btn btn-secondary" onClick={() => setDetailModalOpen(false)}>
                Close
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
