import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';

export default function DangKy() {
  const [hoTen, setHoTen] = useState('');
  const [email, setEmail] = useState('');
  const [matKhau, setMatKhau] = useState('');
  const [soDienThoai, setSoDienThoai] = useState('');
  const [thongBaoLoi, setThongBaoLoi] = useState('');
  const navigate = useNavigate();

  const handleDangKy = async (e) => {
    e.preventDefault();
    setThongBaoLoi('');
    try {
      // Gọi API đăng ký backend (endpoint /api/xac-thuc/dang-ky)
      await api.post('/xac-thuc/dang-ky', {
        hoTen,
        email,
        matKhau,
        soDienThoai
      });
      
      alert('Đăng ký tài khoản thành công! Vui lòng đăng nhập.');
      navigate('/dang-nhap');
    } catch (error) {
      const errorMessage = error.response?.data;
      setThongBaoLoi(typeof errorMessage === 'string' ? errorMessage : 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin!');
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-slate-900">
      <div className="w-full max-w-md p-8 bg-white rounded-2xl shadow-xl">
        <div className="text-center mb-6">
          <span className="text-3xl">🏸</span>
          <h2 className="text-2xl font-bold text-gray-800 mt-2">Đăng Ký Tài Khoản</h2>
          <p className="text-xs text-gray-500 mt-1">Tạo tài khoản để đặt sân cầu lông nhanh chóng</p>
        </div>
        
        {thongBaoLoi && (
          <div className="p-3 mb-4 text-sm text-red-700 bg-red-100 rounded-xl">
            {thongBaoLoi}
          </div>
        )}

        <form onSubmit={handleDangKy} className="space-y-4">
          <div>
            <label className="block mb-1.5 text-xs font-semibold text-gray-600">Họ và tên</label>
            <input
              type="text"
              value={hoTen}
              onChange={(e) => setHoTen(e.target.value)}
              required
              className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              placeholder="Nhập họ và tên của bạn"
            />
          </div>

          <div>
            <label className="block mb-1.5 text-xs font-semibold text-gray-600">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              placeholder="Nhập email"
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

          <div>
            <label className="block mb-1.5 text-xs font-semibold text-gray-600">Số điện thoại</label>
            <input
              type="text"
              value={soDienThoai}
              onChange={(e) => setSoDienThoai(e.target.value)}
              required
              className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              placeholder="Nhập số điện thoại"
            />
          </div>

          <button
            type="submit"
            className="w-full py-3 font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl text-sm shadow-md transition duration-200"
          >
            Đăng Ký
          </button>

          <div className="text-center mt-4">
            <span className="text-xs text-gray-500">Đã có tài khoản? </span>
            <Link to="/dang-nhap" className="text-xs font-bold text-emerald-600 hover:underline">
              Đăng nhập ngay
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}