import client from './client';

export const orderApi = {
  getOrders: async ({ status = '', page = 0, size = 100 } = {}) => {
    const params = { page, size };
    if (status && status.trim()) {
      params.status = status.trim();
    }
    const response = await client.get('/api/orders', { params });
    return response.data;
  },

  getOrderById: async (id) => {
    const response = await client.get(`/api/orders/${id}`);
    return response.data;
  },

  createOrder: async (items) => {
    // items: [{ productId, quantity }]
    const response = await client.post('/api/orders', { items });
    return response.data;
  },

  updateOrderStatus: async (id, status) => {
    const response = await client.patch(`/api/orders/${id}/status`, { status });
    return response.data;
  },
};
