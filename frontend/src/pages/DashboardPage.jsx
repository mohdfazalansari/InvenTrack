import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { productApi } from '../api/productApi';
import { inventoryApi } from '../api/inventoryApi';
import { orderApi } from '../api/orderApi';
import { extractErrorMessage } from '../api/client';
import Alert from '../components/Alert';

export default function DashboardPage({ setCurrentView }) {
  const { user, isAdmin } = useAuth();

  const [stats, setStats] = useState({
    totalProducts: 0,
    lowStockProducts: 0,
    totalOrders: 0,
    pendingOrders: 0,
  });

  const [recentOrders, setRecentOrders] = useState([]);
  const [lowStockList, setLowStockList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchDashboardData = async () => {
    setLoading(true);
    setError('');
    try {
      // 1. Fetch total products count
      const productsData = await productApi.getProducts({ page: 0, size: 1 });
      const totalProducts = productsData?.page?.totalElements ?? productsData?.totalElements ?? 0;

      // 2. Fetch low stock products count & preview
      const lowStockData = await inventoryApi.getLowStock({ page: 0, size: 5 });
      const lowStockProducts = lowStockData?.page?.totalElements ?? lowStockData?.totalElements ?? 0;
      const lowStockItems = lowStockData?.content || [];

      // 3. Fetch total orders count & recent orders
      const ordersData = await orderApi.getOrders({ page: 0, size: 5 });
      const totalOrders = ordersData?.page?.totalElements ?? ordersData?.totalElements ?? 0;
      const recent = ordersData?.content || [];

      // 4. Fetch pending orders count
      const pendingData = await orderApi.getOrders({ status: 'PENDING', page: 0, size: 1 });
      const pendingOrders = pendingData?.page?.totalElements ?? pendingData?.totalElements ?? 0;

      setStats({
        totalProducts,
        lowStockProducts,
        totalOrders,
        pendingOrders,
      });

      setRecentOrders(recent);
      setLowStockList(lowStockItems);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Dashboard Overview</h1>
          <p className="page-subtitle">
            Welcome back, <strong>{user?.username}</strong>! System role: <span className="badge">{user?.role}</span>
          </p>
        </div>
        <div className="header-actions">
          <button className="btn btn-secondary" onClick={fetchDashboardData} disabled={loading}>
            🔄 Refresh Data
          </button>
          <button className="btn btn-primary" onClick={() => setCurrentView('create-order')}>
            ➕ New Order
          </button>
        </div>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError('')} />}

      {/* Metric Cards */}
      <div className="metrics-grid">
        <div className="metric-card" onClick={() => setCurrentView('products')}>
          <div className="metric-icon bg-blue-subtle">📦</div>
          <div className="metric-info">
            <span className="metric-label">Total Products</span>
            <span className="metric-value">{loading ? '...' : stats.totalProducts}</span>
          </div>
        </div>

        <div className="metric-card" onClick={() => setCurrentView('inventory')}>
          <div className="metric-icon bg-amber-subtle">⚠️</div>
          <div className="metric-info">
            <span className="metric-label">Low Stock Alerts</span>
            <span className="metric-value text-warning">{loading ? '...' : stats.lowStockProducts}</span>
          </div>
        </div>

        <div className="metric-card" onClick={() => setCurrentView('orders')}>
          <div className="metric-icon bg-green-subtle">🛒</div>
          <div className="metric-info">
            <span className="metric-label">Total Orders</span>
            <span className="metric-value">{loading ? '...' : stats.totalOrders}</span>
          </div>
        </div>

        <div className="metric-card" onClick={() => setCurrentView('orders')}>
          <div className="metric-icon bg-purple-subtle">⏳</div>
          <div className="metric-info">
            <span className="metric-label">Pending Orders</span>
            <span className="metric-value text-info">{loading ? '...' : stats.pendingOrders}</span>
          </div>
        </div>
      </div>

      {/* Two Column Layout: Low Stock and Recent Orders */}
      <div className="dashboard-grid">
        {/* Low Stock Items */}
        <div className="card">
          <div className="card-header">
            <h3>Low Stock Watchlist</h3>
            <button className="btn-sm btn-link" onClick={() => setCurrentView('inventory')}>
              View Inventory →
            </button>
          </div>
          <div className="card-body">
            {loading ? (
              <p className="text-muted">Loading low stock items...</p>
            ) : lowStockList.length === 0 ? (
              <p className="text-success">✅ All inventory levels are above their thresholds.</p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Current</th>
                    <th>Threshold</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {lowStockList.map((item) => (
                    <tr key={item.productId}>
                      <td><strong>{item.productName}</strong></td>
                      <td>
                        <span className={`badge ${item.quantity === 0 ? 'badge-danger' : 'badge-warning'}`}>
                          {item.quantity} units
                        </span>
                      </td>
                      <td>{item.lowStockThreshold} units</td>
                      <td>
                        <span className="status-indicator status-warning">Low Stock</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Recent Orders */}
        <div className="card">
          <div className="card-header">
            <h3>Recent Orders</h3>
            <button className="btn-sm btn-link" onClick={() => setCurrentView('orders')}>
              View All Orders →
            </button>
          </div>
          <div className="card-body">
            {loading ? (
              <p className="text-muted">Loading recent orders...</p>
            ) : recentOrders.length === 0 ? (
              <p className="text-muted">No orders found. Create your first order to get started!</p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Order #</th>
                    <th>Customer</th>
                    <th>Total</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentOrders.map((ord) => (
                    <tr key={ord.id}>
                      <td><strong>#{ord.id}</strong></td>
                      <td>{ord.username}</td>
                      <td>${Number(ord.totalAmount).toFixed(2)}</td>
                      <td>
                        <span className={`badge badge-status-${ord.status.toLowerCase()}`}>
                          {ord.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
