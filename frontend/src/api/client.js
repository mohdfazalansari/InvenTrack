import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const client = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token
client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle 401 Unauthorized
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Clear stored auth credentials
      localStorage.removeItem('token');
      localStorage.removeItem('user');

      // Dispatch event so AuthContext / App can transition to login view
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
    }
    return Promise.reject(error);
  }
);

// Helper to extract clean error message from backend ErrorResponse
export const extractErrorMessage = (error) => {
  if (error.response?.data) {
    const data = error.response.data;
    if (data.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
      const fieldMsgs = Object.entries(data.fieldErrors)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join(', ');
      return `${data.message || data.error || 'Validation error'}: ${fieldMsgs}`;
    }
    return data.message || data.error || 'An error occurred on the server.';
  }
  return error.message || 'Unable to communicate with the server. Please check backend connection.';
};

export default client;
