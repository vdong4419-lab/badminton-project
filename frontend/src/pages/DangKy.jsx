import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';

export default function DangKy() {
  const [hoTen, setHoTen] = useState('');
  const [email, setEmail] = useState('');
  const [matKhau, setMatKhau] = useState('');
  const [soDienThoai, setSoDienThoai] = useState('');
  const [hienMatKhau, setHienMatKhau] = useState(false);
  const [thongBaoLoi, setThongBaoLoi] = useState('');
  const [dangGui, setDangGui] = useState(false);
  const navigate = useNavigate();

  // Tính toán độ mạnh mật khẩu đơn giản (0 -> 4)
  const tinhDoManhMatKhau = (pass) => {
    let score = 0;
    if (!pass) return score;
    if (pass.length >= 8) score++;
    if (/[A-Z]/.test(pass)) score++;
    if (/[0-9]/.test(pass)) score++;
    if (/[^A-Za-z0-9]/.test(pass)) score++;
    return score;
  };

  const doManh = tinhDoManhMatKhau(matKhau);

  const handleDangKy = async (e) => {
    e.preventDefault();
    if (dangGui) return;
    setDangGui(true);
    setThongBaoLoi('');
    try {
      await api.post('/xac-thuc/dang-ky', {
        hoTen: hoTen.trim(),
        email: email.trim().toLowerCase(),
        matKhau,
        soDienThoai: soDienThoai.trim()
      });

      navigate('/xac-minh-email', { state: { email: email.trim().toLowerCase(), daGui: true, thongBao: 'Đã gửi OTP xác minh. Vui lòng kiểm tra hộp thư và thư rác.' } });
    } catch (error) {
      setThongBaoLoi(layThongBaoLoi(error, 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin!'));
    } finally {
      setDangGui(false);
    }
  };

  return (
    <div className="flex min-h-screen bg-slate-950 font-sans antialiased">
      {/* CỘT TRÁI: BANNER THƯƠNG HIỆU (Chỉ hiện từ màn hình desktop md trở lên) */}
      <div className="hidden md:flex md:w-1/2 relative bg-gradient-to-br from-slate-900 via-indigo-950 to-emerald-950 p-12 flex-col justify-between overflow-hidden border-r border-slate-800">
        {/* Hiệu ứng đốm sáng trang trí background */}
        <div className="absolute -top-24 -left-24 w-96 h-96 bg-emerald-500/20 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-24 -right-24 w-96 h-96 bg-indigo-500/20 rounded-full blur-3xl pointer-events-none" />

        {/* Top Header Logo */}
        <div className="relative z-10 flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 text-slate-950 font-extrabold text-2xl shadow-lg shadow-emerald-500/30">
            🏸
          </div>
          <span className="text-xl font-bold tracking-tight text-white">
            Badminton<span className="text-emerald-400">Hub</span>
          </span>
        </div>

        {/* Middle Hero Text */}
        <div className="relative z-10 space-y-6 max-w-lg">
          <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-semibold uppercase tracking-widest">
            ✨ Hệ Thống Đặt Sân Hiện Đại
          </span>
          <h1 className="text-4xl font-extrabold text-white leading-tight">
            Nâng tầm trải nghiệm thể thao của bạn cùng chúng tôi.
          </h1>
          <p className="text-slate-400 text-sm leading-relaxed">
            Đăng ký tài khoản ngay hôm nay để trải nghiệm dịch vụ đặt sân tự động, chọn khung giờ linh hoạt và quản lý lịch chơi dễ dàng chỉ trong vài bước.
          </p>

          {/* Cards thông số/tính năng nổi bật */}
          <div className="grid grid-cols-2 gap-4 pt-4">
            <div className="p-4 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-md">
              <p className="text-2xl font-bold text-emerald-400">30 Giây</p>
              <p className="text-xs text-slate-400 mt-1">Hoàn tất đặt sân nhanh chóng</p>
            </div>
            <div className="p-4 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-md">
              <p className="text-2xl font-bold text-teal-300">100%</p>
              <p className="text-xs text-slate-400 mt-1">Giữ lịch chính thức không trùng</p>
            </div>
          </div>
        </div>

        {/* Bottom Footer Quote */}
        <div className="relative z-10 text-xs text-slate-500 border-t border-slate-800/80 pt-6">
          © 2026 BadmintonHub Platform. Mọi quyền được bảo lưu.
        </div>
      </div>

      {/* CỘT PHẢI: FORM ĐĂNG KÝ */}
      <div className="w-full md:w-1/2 flex items-center justify-center p-6 sm:p-12 bg-slate-900">
        <div className="w-full max-w-md space-y-8">
          {/* Header Form */}
          <div className="text-center md:text-left space-y-2">
            <h2 className="text-3xl font-extrabold text-white tracking-tight">Tạo tài khoản mới</h2>
            <p className="text-sm text-slate-400">
              Nhập thông tin bên dưới để đăng ký tài khoản trải nghiệm dịch vụ.
            </p>
          </div>

          {/* Thông báo lỗi */}
          {thongBaoLoi && (
            <div className="flex items-center gap-3 p-4 text-sm text-red-300 bg-red-950/50 border border-red-800/60 rounded-2xl animate-shake">
              <span className="text-lg">⚠️</span>
              <p className="flex-1 text-xs leading-relaxed">{thongBaoLoi}</p>
            </div>
          )}

          {/* Form Đăng ký */}
          <form onSubmit={handleDangKy} className="space-y-5">
            {/* Họ và tên */}
            <div>
              <label className="block mb-2 text-xs font-semibold text-slate-300 uppercase tracking-wider">Họ và tên</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 flex items-center pl-4 text-slate-500">👤</span>
                <input
                  type="text"
                  value={hoTen}
                  onChange={(e) => setHoTen(e.target.value)}
                  required
                  className="w-full pl-11 pr-4 py-3 bg-slate-800/60 border border-slate-700/80 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                  placeholder="Nguyễn Văn A"
                />
              </div>
            </div>

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

            {/* Số điện thoại */}
            <div>
              <label className="block mb-2 text-xs font-semibold text-slate-300 uppercase tracking-wider">Số điện thoại</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 flex items-center pl-4 text-slate-500">📞</span>
                <input
                  type="tel"
                  value={soDienThoai}
                  onChange={(e) => setSoDienThoai(e.target.value)}
                  required
                  className="w-full pl-11 pr-4 py-3 bg-slate-800/60 border border-slate-700/80 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                  placeholder="0912345678"
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
                  minLength={8}
                  maxLength={72}
                  value={matKhau}
                  onChange={(e) => setMatKhau(e.target.value)}
                  required
                  className="w-full pl-11 pr-12 py-3 bg-slate-800/60 border border-slate-700/80 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                  placeholder="Tối thiểu 8 ký tự"
                />
                <button
                  type="button"
                  onClick={() => setHienMatKhau(!hienMatKhau)}
                  className="absolute inset-y-0 right-0 flex items-center pr-4 text-xs text-slate-400 hover:text-white"
                >
                  {hienMatKhau ? 'Ẩn' : 'Hiện'}
                </button>
              </div>

              {/* Thanh đo độ mạnh mật khẩu */}
              {matKhau && (
                <div className="mt-2 space-y-1">
                  <div className="flex h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className={`h-full transition-all duration-300 ${
                        doManh <= 1 ? 'w-1/4 bg-red-500' : doManh === 2 ? 'w-2/4 bg-amber-500' : doManh === 3 ? 'w-3/4 bg-teal-400' : 'w-full bg-emerald-500'
                      }`}
                    />
                  </div>
                  <p className="text-[10px] text-right font-medium text-slate-400">
                    Độ an toàn: {doManh <= 1 ? 'Yếu' : doManh === 2 ? 'Trung bình' : doManh === 3 ? 'Khá' : 'Rất mạnh'}
                  </p>
                </div>
              )}
            </div>

            {/* Nút đăng ký Submit */}
            <button
              type="submit"
              disabled={dangGui}
              className="w-full py-3.5 px-4 font-bold text-slate-950 bg-gradient-to-r from-emerald-400 to-teal-400 hover:from-emerald-300 hover:to-teal-300 rounded-xl text-sm shadow-lg shadow-emerald-500/25 active:scale-[0.99] disabled:opacity-50 transition duration-200"
            >
              {dangGui ? 'Đang tạo tài khoản...' : 'Đăng Ký Tải Khoản'}
            </button>

            {/* Chuyển hướng đăng nhập */}
            <div className="text-center pt-2">
              <span className="text-xs text-slate-400">Đã có tài khoản trên hệ thống? </span>
              <Link to="/dang-nhap" className="text-xs font-bold text-emerald-400 hover:text-emerald-300 hover:underline ml-1">
                Đăng nhập ngay
              </Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
