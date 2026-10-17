import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';

export default function DangNhap() {
  const [email, setEmail] = useState('');
  const [matKhau, setMatKhau] = useState('');
  const [thongBaoLoi, setThongBaoLoi] = useState('');
  const [dangGui, setDangGui] = useState(false);
  const navigate = useNavigate();

  const handleDangNhap = async (e) => {
    e.preventDefault();
    if (dangGui) return;
    setDangGui(true);
    setThongBaoLoi('');
    try {
      const response = await api.post('/xac-thuc/dang-nhap', { email: email.trim().toLowerCase(), matKhau });
      const { token, email: userEmail, hoTen } = response.data;
      
      localStorage.setItem('token', token);
      localStorage.setItem('userEmail', userEmail);
      localStorage.setItem('hoTen', hoTen || '');

      alert('Đăng nhập thành công!');
      navigate('/');
    } catch (error) {
      setThongBaoLoi(layThongBaoLoi(error, 'Không thể đăng nhập. Vui lòng thử lại.'));
    } finally {
      setDangGui(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-slate-900">
      <div className="w-full max-w-md p-8 bg-white rounded-2xl shadow-xl">
        <div className="text-center mb-6">
          <span className="text-3xl">🏸</span>
          <h2 className="text-2xl font-bold text-gray-800 mt-2">Đăng Nhập Hệ Thống</h2>
          <p className="text-xs text-gray-500 mt-1">Quản lý và đặt sân cầu lông dễ dàng</p>
        </div>

        {thongBaoLoi && (
          <div className="p-3 mb-4 text-sm text-red-700 bg-red-100 rounded-xl">
            {thongBaoLoi}
          </div>
        )}

        <form onSubmit={handleDangNhap} className="space-y-4">
          <div>
            <label className="block mb-1.5 text-xs font-semibold text-gray-600">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              placeholder="Nhập email của bạn"
            />
          </div>

          <div>
            <label className="block mb-1.5 text-xs font-semibold text-gray-600">Mật khẩu</label>
            <input
              type="password"
              value={matKhau}
              onChange={(e) => setMatKhau(e.target.value)}
              required
              className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              placeholder="Nhập mật khẩu"
            />
          </div>

          <button
            type="submit"
            disabled={dangGui}
            className="w-full py-3 font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl text-sm shadow-md transition duration-200"
          >
            {dangGui ? 'Đang đăng nhập...' : 'Đăng Nhập'}
          </button>

          <div className="text-center mt-4">
            <span className="text-xs text-gray-500">Chưa có tài khoản? </span>
            <Link to="/dang-ky" className="text-xs font-bold text-emerald-600 hover:underline">
              Đăng ký ngay
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
