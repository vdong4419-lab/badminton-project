import React, { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';
import { usePhienDangNhap } from '../context/PhienContext';

export default function DangNhap() {
  const { dangNhap } = usePhienDangNhap();
  const [email, setEmail] = useState('');
  const [matKhau, setMatKhau] = useState('');
  const [hienMatKhau, setHienMatKhau] = useState(false);
  const [thongBaoLoi, setThongBaoLoi] = useState('');
  const [dangGui, setDangGui] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const handleDangNhap = async (e) => {
    e.preventDefault();
    if (dangGui) return;
    setDangGui(true);
    setThongBaoLoi('');
    try {
      const response = await api.post('/xac-thuc/dang-nhap', { email: email.trim().toLowerCase(), matKhau });
      dangNhap(response.data);

      navigate(['ADMIN', 'STAFF'].includes(response.data.vaiTro) ? '/quan-tri' : '/');
    } catch (error) {
      if (error.response?.status === 403) {
        navigate('/xac-minh-email', { state: { email: email.trim().toLowerCase(), thongBao: layThongBaoLoi(error) } });
        return;
      }
      setThongBaoLoi(layThongBaoLoi(error, 'Không thể đăng nhập. Vui lòng kiểm tra lại thông tin.'));
    } finally {
      setDangGui(false);
    }
  };

  return (
    <div className="flex min-h-screen bg-slate-950 font-sans antialiased">
      {/* CỘT TRÁI: BANNER THƯƠNG HIỆU */}
      <div className="hidden md:flex md:w-1/2 relative bg-gradient-to-br from-slate-900 via-indigo-950 to-emerald-950 p-12 flex-col justify-between overflow-hidden border-r border-slate-800">
        <div className="absolute -top-24 -right-24 w-96 h-96 bg-emerald-500/20 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-24 -left-24 w-96 h-96 bg-indigo-500/20 rounded-full blur-3xl pointer-events-none" />

        {/* Top Header Logo */}
        <div className="relative z-10 flex items-center gap-3">
          <Link to="/" className="flex items-center gap-3 group">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 text-slate-950 font-extrabold text-2xl shadow-lg shadow-emerald-500/30 group-hover:scale-105 transition">
              🏸
            </div>
            <span className="text-xl font-bold tracking-tight text-white">
              Badminton<span className="text-emerald-400">Hub</span>
            </span>
          </Link>
        </div>

        {/* Middle Hero Text */}
        <div className="relative z-10 space-y-6 max-w-lg">
          <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-semibold uppercase tracking-widest">
            ⚡ Chào mừng trở lại
          </span>
          <h1 className="text-4xl font-extrabold text-white leading-tight">
            Quản lý sân bãi và lịch đặt nhanh chóng trong tầm tay.
          </h1>
          <p className="text-slate-400 text-sm leading-relaxed">
            Đăng nhập để tiếp tục quản lý các đơn đặt sân của bạn, theo dõi trạng thái thanh toán hoặc truy cập bảng điều khiển hệ thống nếu bạn là quản trị viên.
          </p>
        </div>

        {/* Bottom Footer Quote */}
        <div className="relative z-10 text-xs text-slate-500 border-t border-slate-800/80 pt-6">
          © 2026 BadmintonHub Platform. Nền tảng đặt sân thể thao chuyên nghiệp.
        </div>
      </div>

      {/* CỘT PHẢI: FORM ĐĂNG NHẬP */}
      <div className="w-full md:w-1/2 flex items-center justify-center p-6 sm:p-12 bg-slate-900">
        <div className="w-full max-w-md space-y-8">
          {/* Header Form */}
          <div className="text-center md:text-left space-y-2">
            <h2 className="text-3xl font-extrabold text-white tracking-tight">Đăng nhập tài khoản</h2>
            <p className="text-sm text-slate-400">
              Nhập email và mật khẩu của bạn để truy cập hệ thống.
            </p>
          </div>

          {location.state?.thongBao && <p role="status" className="rounded-xl bg-emerald-950 p-4 text-sm text-emerald-200">{location.state.thongBao}</p>}
          {/* Thông báo lỗi */}
          {thongBaoLoi && (
            <div className="flex items-center gap-3 p-4 text-sm text-red-300 bg-red-950/50 border border-red-800/60 rounded-2xl animate-shake">
              <span className="text-lg">⚠️</span>
              <p className="flex-1 text-xs leading-relaxed">{thongBaoLoi}</p>
            </div>
          )}

          <div className="flex flex-wrap justify-between gap-3 text-sm text-emerald-400"><Link to="/quen-mat-khau">Quên mật khẩu?</Link><Link to="/xac-minh-email">Xác minh email / gửi lại OTP</Link></div>
          {/* Form Đăng nhập */}
          <form onSubmit={handleDangNhap} className="space-y-5">
            {/* Email */}
            <div>
              <label className="block mb-2 text-xs font-semibold text-slate-300 uppercase tracking-wider">Địa chỉ Email</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 flex items-center pl-4 text-slate-500">✉️</span>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="w-full pl-11 pr-4 py-3 bg-slate-800/60 border border-slate-700/80 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                  placeholder="name@example.com"
                />
              </div>
            </div>

            {/* Mật khẩu */}
            <div>
              <label className="block mb-2 text-xs font-semibold text-slate-300 uppercase tracking-wider">Mật khẩu</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 flex items-center pl-4 text-slate-500">🔒</span>
                <input
                  type={hienMatKhau ? 'text' : 'password'}
                  value={matKhau}
                  onChange={(e) => setMatKhau(e.target.value)}
                  required
                  className="w-full pl-11 pr-12 py-3 bg-slate-800/60 border border-slate-700/80 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                  placeholder="Nhập mật khẩu của bạn"
                />
                <button
                  type="button"
                  onClick={() => setHienMatKhau(!hienMatKhau)}
                  className="absolute inset-y-0 right-0 flex items-center pr-4 text-xs text-slate-400 hover:text-white"
                >
                  {hienMatKhau ? 'Ẩn' : 'Hiện'}
                </button>
              </div>
            </div>

            {/* Nút đăng nhập Submit */}
            <button
              type="submit"
              disabled={dangGui}
              className="w-full py-3.5 px-4 font-bold text-slate-950 bg-gradient-to-r from-emerald-400 to-teal-400 hover:from-emerald-300 hover:to-teal-300 rounded-xl text-sm shadow-lg shadow-emerald-500/25 active:scale-[0.99] disabled:opacity-50 transition duration-200"
            >
              {dangGui ? 'Đang xác thực...' : 'Đăng Nhập Ngay'}
            </button>

            {/* Chuyển hướng đăng ký */}
            <div className="text-center pt-2">
              <span className="text-xs text-slate-400">Chưa có tài khoản trên hệ thống? </span>
              <Link to="/dang-ky" className="text-xs font-bold text-emerald-400 hover:text-emerald-300 hover:underline ml-1">
                Đăng ký ngay
              </Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
