import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { productApi } from '../api/productApi';
import { extractErrorMessage } from '../api/client';
import Alert from '../components/Alert';
import Modal from '../components/Modal';

export default function ProductsPage() {
  const { isAdmin } = useAuth();

  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState('add'); // 'add' | 'edit'
  const [selectedProductId, setSelectedProductId] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    category: '',
    price: '',
    quantity: '',
    lowStockThreshold: '',
  });
  const [formSubmitting, setFormSubmitting] = useState(false);
  const [modalError, setModalError] = useState('');

  // Delete confirmation modal state
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [productToDelete, setProductToDelete] = useState(null);

  const fetchProducts = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await productApi.getProducts({ category: categoryFilter, size: 100 });
      setProducts(data.content || []);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, [categoryFilter]);

  const handleOpenAdd = () => {
    setModalMode('add');
    setFormData({
      name: '',
      description: '',
      category: '',
      price: '',
      quantity: '',
      lowStockThreshold: '5',
    });
    setModalError('');
    setIsModalOpen(true);
  };

  const handleOpenEdit = (product) => {
    setModalMode('edit');
    setSelectedProductId(product.id);
    setFormData({
      name: product.name,
      description: product.description || '',
      category: product.category,
      price: product.price,
      quantity: product.quantity,
      lowStockThreshold: product.lowStockThreshold,
    });
    setModalError('');
    setIsModalOpen(true);
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    setModalError('');

    const payload = {
      name: formData.name.trim(),
      description: formData.description?.trim() || '',
      category: formData.category.trim(),
      price: parseFloat(formData.price),
      quantity: parseInt(formData.quantity, 10),
      lowStockThreshold: parseInt(formData.lowStockThreshold, 10),
    };

    if (!payload.name || !payload.category || isNaN(payload.price) || isNaN(payload.quantity)) {
      setModalError('Please fill out all required fields with valid values.');
      return;
    }

    setFormSubmitting(true);
    try {
      if (modalMode === 'add') {
        await productApi.createProduct(payload);
        setSuccess('Product successfully added to catalog.');
      } else {
        await productApi.updateProduct(selectedProductId, payload);
        setSuccess('Product details successfully updated.');
      }
      setIsModalOpen(false);
      fetchProducts();
    } catch (err) {
      setModalError(extractErrorMessage(err));
    } finally {
      setFormSubmitting(false);
    }
  };

  const handleOpenDelete = (product) => {
    setProductToDelete(product);
    setDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!productToDelete) return;
    try {
      await productApi.deleteProduct(productToDelete.id);
      setSuccess(`Product '${productToDelete.name}' has been soft deleted.`);
      setDeleteModalOpen(false);
      setProductToDelete(null);
      fetchProducts();
    } catch (err) {
      setError(extractErrorMessage(err));
      setDeleteModalOpen(false);
    }
  };

  // Distinct categories for filter dropdown
  const categories = Array.from(new Set(products.map((p) => p.category))).filter(Boolean);

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Product Catalog</h1>
          <p className="page-subtitle">View and manage items in the inventory catalog</p>
        </div>
        <div className="header-actions">
          {isAdmin && (
            <button className="btn btn-primary" onClick={handleOpenAdd}>
              ➕ Add Product
            </button>
          )}
        </div>
      </div>

      {success && <Alert type="success" message={success} onClose={() => setSuccess('')} />}
      {error && <Alert type="error" message={error} onClose={() => setError('')} />}

      <div className="card">
        <div className="card-header flex-between">
          <div className="filter-group">
            <label htmlFor="cat-filter">Filter by Category:</label>
            <select
              id="cat-filter"
              className="form-control-sm"
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
            >
              <option value="">All Categories</option>
              {categories.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
          <button className="btn btn-secondary btn-sm" onClick={fetchProducts} disabled={loading}>
            🔄 Refresh
          </button>
        </div>

        <div className="card-body p-0">
          {loading ? (
            <div className="p-4 text-center text-muted">Loading products catalog...</div>
          ) : products.length === 0 ? (
            <div className="p-4 text-center text-muted">No products found.</div>
          ) : (
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Category</th>
                    <th>Price</th>
                    <th>Stock</th>
                    <th>Threshold</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <strong>{p.name}</strong>
                        {p.description && <div className="text-muted small">{p.description}</div>}
                      </td>
                      <td>
                        <span className="category-pill">{p.category}</span>
                      </td>
                      <td>${Number(p.price).toFixed(2)}</td>
                      <td>
                        <span
                          className={`badge ${
                            p.quantity === 0
                              ? 'badge-danger'
                              : p.quantity <= p.lowStockThreshold
                              ? 'badge-warning'
                              : 'badge-success'
                          }`}
                        >
                          {p.quantity} units
                        </span>
                      </td>
                      <td>{p.lowStockThreshold} units</td>
                      <td>
                        <div className="action-buttons">
                          {isAdmin ? (
                            <>
                              <button
                                className="btn-icon btn-edit"
                                title="Edit Product"
                                onClick={() => handleOpenEdit(p)}
                              >
                                ✏️ Edit
                              </button>
                              <button
                                className="btn-icon btn-delete"
                                title="Delete Product"
                                onClick={() => handleOpenDelete(p)}
                              >
                                🗑️ Delete
                              </button>
                            </>
                          ) : (
                            <span className="text-muted small">View Only</span>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Add / Edit Product Modal */}
      <Modal
        isOpen={isModalOpen}
        title={modalMode === 'add' ? 'Add New Product' : 'Edit Product'}
        onClose={() => setIsModalOpen(false)}
      >
        {modalError && <Alert type="error" message={modalError} onClose={() => setModalError('')} />}
        <form onSubmit={handleFormSubmit}>
          <div className="form-group">
            <label htmlFor="prod-name">Product Name *</label>
            <input
              id="prod-name"
              type="text"
              className="form-control"
              placeholder="e.g. Wireless Mouse"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="prod-desc">Description</label>
            <textarea
              id="prod-desc"
              rows="2"
              className="form-control"
              placeholder="Optional description"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>

          <div className="form-row">
            <div className="form-group col">
              <label htmlFor="prod-cat">Category *</label>
              <input
                id="prod-cat"
                type="text"
                className="form-control"
                placeholder="e.g. Peripherals"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                required
              />
            </div>
            <div className="form-group col">
              <label htmlFor="prod-price">Price ($) *</label>
              <input
                id="prod-price"
                type="number"
                step="0.01"
                min="0.01"
                className="form-control"
                placeholder="29.99"
                value={formData.price}
                onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                required
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group col">
              <label htmlFor="prod-qty">Initial Quantity *</label>
              <input
                id="prod-qty"
                type="number"
                min="0"
                className="form-control"
                placeholder="50"
                value={formData.quantity}
                onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
                required
              />
            </div>
            <div className="form-group col">
              <label htmlFor="prod-thresh">Low Stock Threshold *</label>
              <input
                id="prod-thresh"
                type="number"
                min="0"
                className="form-control"
                placeholder="5"
                value={formData.lowStockThreshold}
                onChange={(e) => setFormData({ ...formData, lowStockThreshold: e.target.value })}
                required
              />
            </div>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsModalOpen(false)}
              disabled={formSubmitting}
            >
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={formSubmitting}>
              {formSubmitting ? 'Saving...' : modalMode === 'add' ? 'Create Product' : 'Save Changes'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={deleteModalOpen}
        title="Confirm Soft Deletion"
        onClose={() => setDeleteModalOpen(false)}
      >
        <p>
          Are you sure you want to delete product <strong>"{productToDelete?.name}"</strong>?
        </p>
        <p className="text-muted small">
          This performs a soft delete in the database to preserve historical order records and prevent broken references.
        </p>
        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={() => setDeleteModalOpen(false)}>
            Cancel
          </button>
          <button className="btn btn-danger" onClick={handleConfirmDelete}>
            Yes, Delete Product
          </button>
        </div>
      </Modal>
    </div>
  );
}
