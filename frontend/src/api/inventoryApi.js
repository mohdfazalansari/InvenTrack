import client from './client';

export const inventoryApi = {
  getInventory: async ({ page = 0, size = 100 } = {}) => {
    const response = await client.get('/api/inventory', {
      params: { page, size },
    });
    return response.data;
  },

  getLowStock: async ({ page = 0, size = 100 } = {}) => {
    const response = await client.get('/api/inventory/low-stock', {
      params: { page, size },
    });
    return response.data;
  },

  adjustStock: async (productId, { quantityDelta, reason = '' }) => {
    const response = await client.post(`/api/inventory/${productId}/adjust`, {
      quantityDelta: Number(quantityDelta),
      reason,
    });
    return response.data;
  },
};
