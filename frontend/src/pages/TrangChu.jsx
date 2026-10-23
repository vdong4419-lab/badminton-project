import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';
import { usePhienDangNhap } from '../context/PhienContext';
import LienKetQuanTri from '../components/LienKetQuanTri';
import ChuongThongBao from '../components/ChuongThongBao';
import { tienViet } from '../utils/hienThi';
import DiemDanhGia from '../components/DiemDanhGia';

export default function TrangChu() {
  const navigate = useNavigate();
  const { nguoiDung, dangXuat } = usePhienDangNhap();
  const userEmail = nguoiDung?.email;
  const [thongBaoLoi, setThongBaoLoi] = useState('');
  const [danhSachSan, setDanhSachSan] = useState([]);
  const [dangTai, setDangTai] = useState(true);

  // State cho từ khóa tìm kiếm nhanh
  const [tuKhoa, setTuKhoa] = useState('');

  // State kiểm tra hiển thị màn hình chào mừng (Welcome Animation)
  const [showWelcome, setShowWelcome] = useState(() => !sessionStorage.getItem('hasSeenWelcome'));

  useEffect(() => {
    // Kiểm tra xem trong phiên làm việc hiện tại đã xem màn hình chào mừng chưa
    const hasSeenWelcome = sessionStorage.getItem('hasSeenWelcome');
    if (!hasSeenWelcome) {
      const timer = setTimeout(() => {
        setShowWelcome(false);
        sessionStorage.setItem('hasSeenWelcome', 'true');
      }, 2300); // Ẩn màn hình chào sau 2.3 giây

      return () => clearTimeout(timer);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let running = false;
    async function taiDanhSachSan() {
      if (running) return;
      running = true;
      try {
        const response = await api.get('/san', { signal: controller.signal });
        if (!controller.signal.aborted) { setDanhSachSan(response.data); setThongBaoLoi(''); }
      } catch (error) {
        if (!controller.signal.aborted) setThongBaoLoi(layThongBaoLoi(error, 'Không thể tải danh sách sân.'));
      } finally {
        running = false;
        if (!controller.signal.aborted) setDangTai(false);
      }
    }
    void taiDanhSachSan();
    const timer = setInterval(taiDanhSachSan, 30000);
    window.addEventListener('focus', taiDanhSachSan);
    return () => { controller.abort(); clearInterval(timer); window.removeEventListener('focus', taiDanhSachSan); };
  }, []);

  const handleDangXuat = () => {
    dangXuat();
    navigate('/');
  };

  const danhSachLoc = danhSachSan.filter(san => 
    san.tenSan.toLowerCase().includes(tuKhoa.toLowerCase()) || 
    (san.moTa && san.moTa.toLowerCase().includes(tuKhoa.toLowerCase()))
  );

  return (
    <div className="relative flex min-h-screen bg-slate-50 font-sans antialiased overflow-x-hidden">
      
      {/* ================= INJECT CUSTOM KEYFRAMES CHO ANIMATION CHÀO MỪNG ================= */}
      <style>{`
        @keyframes welcomeSlideUp {
          0% { transform: translateY(0); }
          100% { transform: translateY(-100%); }
        }
        @keyframes welcomeSlideDown {
          0% { transform: translateY(0); }
          100% { transform: translateY(100%); }
        }
        @keyframes welcomeFadeIn {
          0% { opacity: 0; transform: scale(0.85); }
          100% { opacity: 1; transform: scale(1); }
        }
        .animate-welcome-slide-up {
          animation: welcomeSlideUp 0.85s cubic-bezier(0.77, 0, 0.175, 1) 1.5s forwards;
        }
        .animate-welcome-slide-down {
          animation: welcomeSlideDown 0.85s cubic-bezier(0.77, 0, 0.175, 1) 1.5s forwards;
        }
        .animate-welcome-fade-in {
          animation: welcomeFadeIn 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.2s both;
        }
        .animate-welcome-subtext {
          animation: welcomeFadeIn 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.5s both;
        }
      `}</style>

      {/* ================= WELCOME SCREEN ANIMATION OVERLAY ================= */}
      {showWelcome && (
        <div className="fixed inset-0 z-50 flex items-center justify-center pointer-events-none">
          {/* Nửa cánh cửa trên */}
          <div className="absolute top-0 left-0 w-full h-1/2 bg-slate-950 border-b border-emerald-500/30 flex items-end justify-center pb-5 animate-welcome-slide-up shadow-2xl">
            <div className="animate-welcome-fade-in flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 text-slate-950 font-black text-2xl shadow-lg shadow-emerald-500/40 animate-bounce">
                🏸
              </div>
              <div>
                <span className="font-extrabold text-2xl tracking-wider text-white block">
                  BADMINTON<span className="text-emerald-400">HUB</span>
                </span>
              </div>
            </div>
          </div>

          {/* Nửa cánh cửa dưới */}
          <div className="absolute bottom-0 left-0 w-full h-1/2 bg-slate-950 border-t border-emerald-500/30 flex items-start justify-center pt-5 animate-welcome-slide-down shadow-2xl">
            <p className="text-xs text-emerald-400 font-semibold uppercase tracking-widest animate-welcome-subtext flex items-center gap-2">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-ping" />
              Chào mừng bạn đến với hệ thống đặt sân 24/7
            </p>
          </div>
        </div>
      )}

      {/* ================= SIDEBAR NAVIGATION - TONE ĐEN SANG TRỌNG ================= */}
      <aside className="w-64 bg-slate-900 text-white flex-col hidden md:flex border-r border-slate-800">
        <div className="p-6 flex items-center space-x-3 border-b border-slate-800/80">
          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 text-slate-950 font-black text-xl shadow-lg shadow-emerald-500/20">
            🏸
          </div>
          <div>
            <span className="font-bold text-base tracking-tight text-white block">BADMINTON<span className="text-emerald-400">HUB</span></span>
            <span className="text-[10px] text-slate-400 block font-medium uppercase tracking-wider">Hệ thống đặt sân 24/7</span>
          </div>
        </div>

        <nav className="flex-1 px-4 py-6 space-y-2">
          <button 
            onClick={() => navigate('/')} 
            className="w-full flex items-center space-x-3 px-4 py-3 bg-gradient-to-r from-emerald-500 to-teal-500 text-slate-950 rounded-2xl font-bold shadow-lg shadow-emerald-500/20 text-left transition"
          >
            <span className="text-lg">🏠</span>
            <span className="text-sm">Trang Chủ</span>
          </button>
          
          <button 
            onClick={() => navigate('/lich-su')} 
            className="w-full flex items-center space-x-3 px-4 py-3 text-slate-400 hover:bg-slate-800/80 hover:text-white rounded-2xl font-medium transition text-left"
          >
            <span className="text-lg">📅</span>
            <span className="text-sm">Lịch Sử Đặt Sân</span>
          </button>

          <Link 
            to="/ca-nhan" 
            className="flex items-center space-x-3 px-4 py-3 text-slate-400 hover:bg-slate-800/80 hover:text-white rounded-2xl font-medium transition"
          >
            <span className="text-lg">👤</span>
            <span className="text-sm">Trang Cá Nhân</span>
          </Link>
        </nav>

        {/* THÔNG TIN TÀI KHOẢN DƯỚI SIDEBAR */}
        {nguoiDung && (
          <div className="p-4 m-4 rounded-2xl bg-slate-800/60 border border-slate-700/60 space-y-2">
            <div className="flex items-center space-x-2">
              <span className="h-2 w-2 rounded-full bg-emerald-400 animate-ping" />
              <p className="text-xs font-semibold text-slate-300 truncate">{nguoiDung.hoTen || userEmail}</p>
            </div>
            <p className="text-[10px] text-slate-400">Vai trò: <strong className="text-emerald-400">{nguoiDung.vaiTro || 'Khách hàng'}</strong></p>
          </div>
        )}
      </aside>

      {/* ================= MAIN CONTENT AREA ================= */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* TOPBAR HEADER */}
        <header className="bg-white border-b border-slate-200/80 px-6 py-4 flex flex-wrap gap-3 items-center justify-between sticky top-0 z-30 shadow-xs backdrop-blur-md bg-white/90">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-widest hidden sm:inline">Trang chủ /</span>
            <span className="text-sm font-extrabold text-slate-800">Đặt Sân Cầu Lông</span>
          </div>

          <div className="flex items-center space-x-3">
            <ChuongThongBao />
            <LienKetQuanTri />

            {userEmail ? (
              <div className="flex items-center space-x-3">
                <Link to="/ca-nhan" className="hidden sm:flex items-center gap-2 text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 px-3.5 py-2 rounded-xl transition">
                  👤 <span>{nguoiDung.hoTen || userEmail}</span>
                </Link>
                <button 
                  onClick={handleDangXuat}
                  className="px-4 py-2 text-xs font-bold bg-rose-500 hover:bg-rose-600 text-white rounded-xl transition shadow-sm active:scale-95"
                >
                  Đăng Xuất
                </button>
              </div>
            ) : (
              <div className="flex items-center space-x-2">
                <button 
                  onClick={() => navigate('/dang-nhap')}
                  className="px-4 py-2 text-xs font-bold text-slate-700 hover:bg-slate-100 border border-slate-200 rounded-xl transition"
                >
                  Đăng Nhập
                </button>
                <button 
                  onClick={() => navigate('/dang-ky')}
                  className="px-4 py-2 text-xs font-bold bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-600 hover:to-teal-600 text-white rounded-xl shadow-md shadow-emerald-500/20 transition active:scale-95"
                >
                  Đăng Ký
                </button>
              </div>
            )}
          </div>
        </header>

        {/* MAIN BODY CONTENT */}
        <main className="p-6 md:p-8 space-y-8 max-w-7xl mx-auto w-full">
          {thongBaoLoi && (
            <div role="alert" className="p-4 text-xs font-medium text-red-700 bg-red-50 border border-red-200 rounded-2xl flex items-center gap-2">
              ⚠️ <span>{thongBaoLoi}</span>
            </div>
          )}

          {/* HERO BANNER SANG TRỌNG */}
          <div className="relative rounded-3xl bg-gradient-to-br from-slate-900 via-indigo-950 to-emerald-950 p-8 md:p-12 text-white shadow-xl overflow-hidden border border-slate-800">
            {/* Hiệu ứng đốm sáng trang trí background */}
            <div className="absolute -top-24 -right-24 w-96 h-96 bg-emerald-500/20 rounded-full blur-3xl pointer-events-none" />
            <div className="absolute -bottom-24 -left-24 w-96 h-96 bg-indigo-500/20 rounded-full blur-3xl pointer-events-none" />

            <div className="relative z-10 max-w-2xl space-y-4">
              <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-bold uppercase tracking-wider">
                ⚡ ĐẶT SÂN NHANH CHỈ TRONG 30 GIÂY
              </span>
              <h1 className="text-3xl md:text-5xl font-extrabold tracking-tight leading-tight">
                Trải Nghiệm Sân Cầu Lông <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 to-teal-300">Đạt Chuẩn BWF</span>
              </h1>
              <p className="text-slate-300 text-xs md:text-sm leading-relaxed">
                Hệ thống thảm Yonex cao cấp, ánh sáng chống chói chuẩn thi đấu, không gian rộng rãi cùng đầy đủ tiện ích nước uống & dịch vụ đi kèm.
              </p>

              {/* BỘ TÌM KIẾM SÂN NHANH TRÊN HERO BANNER */}
              <div className="pt-4 flex flex-col sm:flex-row items-center gap-3">
                <div className="relative w-full sm:w-80">
                  <span className="absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400">🔍</span>
                  <input
                    type="text"
                    value={tuKhoa}
                    onChange={(e) => setTuKhoa(e.target.value)}
                    placeholder="Tìm theo tên sân hoặc vị trí..."
                    className="w-full pl-10 pr-4 py-3 bg-white/10 backdrop-blur-md border border-white/20 rounded-2xl text-xs text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-400"
                  />
                </div>
                <div className="flex items-center gap-4 text-xs text-slate-300 font-semibold pl-2">
                  <span>🏸 {danhSachSan.length} sân đang hoạt động</span>
                  <span>★ Đánh giá từ khách đã chơi</span>
                </div>
              </div>
            </div>
          </div>

          {/* DANH SÁCH SÂN THỂ THAO */}
          <div className="space-y-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-200/80 pb-4">
              <div>
                <h2 className="text-xl font-extrabold text-slate-900 tracking-tight">Danh Sách Sân Cầu Lông</h2>
                <p className="text-xs text-slate-500 mt-0.5">Lựa chọn sân phù hợp và bấm đặt lịch trực tiếp</p>
              </div>
              <span className="text-xs font-bold px-3 py-1.5 rounded-xl bg-slate-100 text-slate-700">
                Hiển thị {danhSachLoc.length} sân
              </span>
            </div>

            {dangTai ? (
              <div className="flex items-center justify-center p-16 bg-white rounded-3xl border border-slate-200 shadow-xs">
                <div className="flex items-center gap-3 text-slate-500 text-xs font-semibold">
                  <span className="h-4 w-4 rounded-full bg-emerald-500 animate-ping" />
                  <span>Đang tải danh sách sân cầu lông...</span>
                </div>
              </div>
            ) : danhSachLoc.length === 0 ? (
              <div className="p-12 text-center bg-white rounded-3xl border border-slate-200 text-slate-400 text-xs font-medium">
                Chưa tìm thấy sân nào phù hợp với từ khóa "{tuKhoa}".
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {danhSachLoc.map((san) => (
                  <div 
                    key={san.id} 
                    className="group bg-white rounded-3xl border border-slate-200/80 overflow-hidden shadow-xs hover:shadow-xl hover:border-slate-300 transition-all duration-300 flex flex-col justify-between"
                  >
                    <div>
                      {/* Hình ảnh sân & Badges */}
                      <div className="relative h-48 bg-slate-100 overflow-hidden">
                        <img 
                          src={san.anhDaiDien || "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=600&auto=format&fit=crop&q=80"}
                          alt={san.tenSan} 
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" 
                        />
                        <div className="absolute top-3 left-3 bg-slate-900/80 backdrop-blur-md text-emerald-400 px-3 py-1 rounded-xl text-[10px] font-bold shadow-md border border-slate-700/50">
                          {san.loaiSan || 'Tiêu chuẩn BWF'}
                        </div>
                        {san.giaMoiGio && (
                          <div className="absolute bottom-3 right-3 bg-emerald-600 text-white px-3 py-1 rounded-xl text-xs font-black shadow-lg">
                            {tienViet(san.giaMoiGio)} / giờ
                          </div>
                        )}
                      </div>

                      {/* Nội dung sân */}
                      <div className="p-5 space-y-2">
                        <p className="text-[11px] text-slate-400 font-semibold uppercase tracking-wider flex items-center gap-1">
                          📍 {san.moTa || 'Khu vực thi đấu chính'}
                        </p>
                        <h3 className="font-bold text-slate-900 text-base group-hover:text-emerald-600 transition line-clamp-1"><Link to={`/san/${san.id}`}>{san.tenSan}</Link></h3>
                        <Link to={`/san/${san.id}#danh-gia`} className="block rounded-lg py-1 hover:underline" title={`Xem đánh giá của ${san.tenSan}`}><DiemDanhGia diem={san.diemTrungBinh} soLuong={san.tongDanhGia} /></Link>
                        {san.danhGiaMoiNhat && <blockquote className="rounded-xl border border-slate-100 bg-slate-50 p-3">
                          <p className="mb-1 text-[10px] font-semibold uppercase tracking-wide text-slate-400">Nhận xét mới nhất</p>
                          <p className="line-clamp-2 break-words text-xs leading-5 text-slate-600">“{san.danhGiaMoiNhat.binhLuan}”</p>
                          <footer className="mt-2 flex items-center justify-between gap-2 text-[11px]"><span className="truncate font-semibold text-slate-500">{san.danhGiaMoiNhat.khachHang}</span><span className="shrink-0 font-semibold text-amber-700">★ {san.danhGiaMoiNhat.diem}/5</span></footer>
                        </blockquote>}
                        <Link to={`/san/${san.id}#danh-gia`} className="inline-block text-xs font-semibold text-emerald-700">Đọc bình luận & thông tin sân →</Link>
                        <p className="text-xs text-slate-500 line-clamp-2 leading-relaxed">
                          Thảm Yonex 5mm chống trượt, đèn LED chống chói tiêu chuẩn thi đấu quốc tế.
                        </p>
                      </div>
                    </div>

                    {/* Footer Thẻ sân & Nút đặt */}
                    <div className="p-5 pt-0">
                      <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
                        <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-600">
                          <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse" /> Sẵn sàng
                        </span>
                        
                        <button 
                          onClick={() => {
                            const token = localStorage.getItem('token');
                            if (!token) {
                              alert('Vui lòng đăng nhập để tiến hành chọn suất đặt sân!');
                              navigate('/dang-nhap');
                            } else {
                              navigate(`/dat-lich/${san.id}`);
                            }
                          }}
                          className="px-4 py-2.5 bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-600 hover:to-teal-600 text-white font-bold rounded-xl text-xs shadow-md shadow-emerald-500/20 active:scale-95 transition flex items-center gap-1 group-hover:translate-x-0.5"
                        >
                          <span>Đặt Sân Ngay</span>
                          <span>→</span>
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* SECTION PHỤ: TIỆN ÍCH ĐI KÈM & DỊCH VỤ */}
          <div className="rounded-3xl bg-white border border-slate-200/80 p-8 shadow-xs space-y-6">
            <h3 className="text-lg font-extrabold text-slate-900 tracking-tight">Dịch Vụ & Tiện Ích Đỉnh Cao</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-1">
                <span className="text-2xl">🚗</span>
                <p className="text-xs font-bold text-slate-800">Bãi Đỗ Xe Rộng Rãi</p>
                <p className="text-[11px] text-slate-500">Miễn phí cho ô tô và xe máy</p>
              </div>
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-1">
                <span className="text-2xl">🥤</span>
                <p className="text-xs font-bold text-slate-800">Căn Tin & Nước Giải Khát</p>
                <p className="text-[11px] text-slate-500">Phục vụ nước điện giải & đồ ăn nhẹ</p>
              </div>
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-1">
                <span className="text-2xl">🏸</span>
                <p className="text-xs font-bold text-slate-800">Cho Thuê Dụng Cụ</p>
                <p className="text-[11px] text-slate-500">Cho thuê vợt Yonex, Victor & bán cầu</p>
              </div>
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-1">
                <span className="text-2xl">📶</span>
                <p className="text-xs font-bold text-slate-800">Wifi Tốc Độ Cao</p>
                <p className="text-[11px] text-slate-500">Phủ sóng toàn bộ khuôn viên sân</p>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
