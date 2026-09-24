import client from './client';

export const productApi = {
  getProducts: async ({ category = '', page = 0, size = 100 } = {}) => {
    const params = { page, size };
    if (category && category.trim()) {
      params.category = category.trim();
    }
    const response = await client.get('/api/products', { params });
    return response.data;
  },

  getProductById: async (id) => {
    const response = await client.get(`/api/products/${id}`);
    return response.data;
  },

  createProduct: async (productData) => {
    // productData: { name, description, category, price, quantity, lowStockThreshold }
    const response = await client.post('/api/products', productData);
    return response.data;
  },

  updateProduct: async (id, productData) => {
    const response = await client.put(`/api/products/${id}`, productData);
    return response.data;
  },

  deleteProduct: async (id) => {
    const response = await client.delete(`/api/products/${id}`);
    return response.data;
  },
};
