import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';
import { phienConHan } from '../utils/hienThi';

export default function TrangChu() {
  const navigate = useNavigate();
  const [userEmail, setUserEmail] = useState(() => phienConHan() ? localStorage.getItem('userEmail') : null);
  const [thongBaoLoi, setThongBaoLoi] = useState('');
  const [danhSachSan, setDanhSachSan] = useState([]);
  const [dangTai, setDangTai] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    async function taiDanhSachSan() {
      try {
        const response = await api.get('/san', { signal: controller.signal });
        if (!controller.signal.aborted) setDanhSachSan(response.data);
      } catch (error) {
        if (!controller.signal.aborted) setThongBaoLoi(layThongBaoLoi(error, 'Không thể tải danh sách sân.'));
      } finally {
        if (!controller.signal.aborted) setDangTai(false);
      }
    }
    void taiDanhSachSan();
    return () => controller.abort();
  }, []);

  const handleDangXuat = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('hoTen');
    setUserEmail(null);
    alert('Đã đăng xuất thành công!');
    navigate('/');
  };

  return (
    <div className="flex min-h-screen bg-[#f3f4f6]">
      <aside className="w-64 bg-[#0f172a] text-white flex flex-col hidden md:flex">
        <div className="p-5 flex items-center space-x-3 border-b border-gray-800">
          <div className="bg-emerald-500 p-2 rounded-lg font-black text-xl">🏸</div>
          <span className="font-bold text-lg tracking-wide">SÂN CẦU LÔNG</span>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1">
          <button 
            onClick={() => navigate('/')} 
            className="w-full flex items-center space-x-3 px-4 py-3 bg-emerald-600 text-white rounded-xl font-medium shadow-sm text-left"
          >
            <span>🏠</span>
            <span>Trang Chủ</span>
          </button>
          <button 
            onClick={() => navigate('/lich-su')} 
            className="w-full flex items-center space-x-3 px-4 py-3 text-gray-400 hover:bg-gray-800 hover:text-white rounded-xl font-medium transition text-left"
          >
            <span>📅</span>
            <span>Sân Đã Đặt</span>
          </button>
        </nav>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="bg-white border-b px-6 py-3 flex items-center justify-between sticky top-0 z-20 shadow-xs">
          <div className="text-sm font-semibold text-gray-600">Hệ Thống Đặt Sân Cầu Lông</div>
          <div className="flex items-center space-x-3">
            {userEmail ? (
              <div className="flex items-center space-x-3">
                <span className="text-xs font-bold text-gray-700 bg-gray-100 px-3 py-1.5 rounded-lg">
                  👤 {userEmail}
                </span>
                <button 
                  onClick={handleDangXuat}
                  className="px-3 py-1.5 text-xs font-semibold bg-red-500 text-white rounded-lg hover:bg-red-600 transition shadow-xs"
                >
                  Đăng Xuất
                </button>
              </div>
            ) : (
              <>
                <button 
                  onClick={() => navigate('/dang-nhap')}
                  className="px-4 py-1.5 text-sm font-semibold border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition"
                >
                  Đăng Nhập
                </button>
                <button 
                  onClick={() => navigate('/dang-ky')}
                  className="px-4 py-1.5 text-sm font-semibold bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition shadow-sm"
                >
                  Đăng Ký
                </button>
              </>
            )}
          </div>
        </header>

        <main className="p-6 space-y-6">
          {thongBaoLoi && <div role="alert" className="p-4 text-red-700 bg-red-100 rounded-xl">{thongBaoLoi}</div>}
          <div className="relative bg-gradient-to-r from-[#064e3b] via-[#047857] to-[#10b981] rounded-2xl p-8 text-white shadow-lg overflow-hidden">
            <div className="relative z-10 max-w-xl">
              <span className="bg-emerald-800/60 text-emerald-200 text-xs px-3 py-1 rounded-full font-semibold uppercase tracking-wider border border-emerald-600/40">
                ⚡ GIẢI SÂN CỰC NHANH - GIỜ CHƠI SIÊU TIỆN
              </span>
              <h1 className="text-3xl lg:text-4xl font-extrabold mt-3 mb-3 leading-tight">
                Hệ Thống Sân Cầu Lông Hiện Đại Hàng Đầu
              </h1>
              <p className="text-emerald-100 text-sm mb-6 leading-relaxed">
                Trải nghiệm tiêu chuẩn nhất với cơ sở vật chất hiện đại, hệ thống đèn LED chống chói mắt, phục vụ nước uống & dụng cụ thi đấu đầy đủ.
              </p>
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-gray-900">Danh Sách Sân Cầu Lông</h2>
            </div>

            {dangTai ? (
              <div className="text-center py-10 text-gray-400 text-sm">Đang tải danh sách sân...</div>
            ) : danhSachSan.length === 0 ? (
              <div className="text-center py-10 text-gray-400 text-sm">Chưa có sân nào trong hệ thống.</div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
                {danhSachSan.map((san) => (
                  <div key={san.id} className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition flex flex-col">
                    <div className="relative h-40 bg-gray-100">
                      <img 
                        src={san.anhDaiDien || "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=500&auto=format&fit=crop&q=60"}
                        alt={san.tenSan} 
                        className="w-full h-full object-cover" 
                      />
                      <div className="absolute top-2 left-2 bg-emerald-600 text-white px-2 py-0.5 rounded-md text-[10px] font-bold shadow">
                        {san.loaiSan || 'Tiêu chuẩn'}
                      </div>
                    </div>

                    <div className="p-4 flex-1 flex flex-col justify-between">
                      <div>
                        <p className="text-[11px] text-gray-400 font-medium mb-1">📍 {san.moTa || 'Khu vực chính'}</p>
                        <h3 className="font-bold text-gray-900 text-sm mb-1 line-clamp-1">{san.tenSan}</h3>
                      </div>

                      <div className="flex items-center justify-between pt-2 border-t border-gray-100 mt-3">
                        <span className="text-emerald-600 font-bold text-xs">Sẵn sàng</span>
                        <button 
                          onClick={() => {
                            const token = localStorage.getItem('token');
                            if (!token) {
                              alert('Vui lòng đăng nhập để tiến hành đặt sân!');
                              navigate('/dang-nhap');
                            } else {
                              navigate(`/dat-lich/${san.id}`);
                            }
                          }}
                          className="px-3 py-1.5 bg-amber-500 hover:bg-amber-600 text-white font-bold rounded-lg text-xs shadow-xs transition"
                        >
                          Đặt Sân
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
