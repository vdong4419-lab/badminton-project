import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8081/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Tự động gắn JWT token vào header trước khi gửi request nếu đã đăng nhập
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export function layThongBaoLoi(error, fallback = 'Không thể thực hiện yêu cầu. Vui lòng thử lại.') {
  const data = error.response?.data;
  return typeof data === 'string' && data ? data : data?.message || fallback;
}

api.interceptors.response.use(response => response, error => {
  if (error.response?.status === 401 && !error.config?.url?.includes('/xac-thuc/')) {
    ['token', 'userEmail', 'hoTen'].forEach(key => localStorage.removeItem(key));
    if (window.location.pathname !== '/dang-nhap') window.location.assign('/dang-nhap');
  }
  return Promise.reject(error);
});

export default api;
