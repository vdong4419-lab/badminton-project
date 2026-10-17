import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';
import { ngayHomNay, tienViet } from '../utils/hienThi';

export default function TrangDatLich() {
  const { sanId } = useParams();
  const navigate = useNavigate();
  
  const [ngayChon, setNgayChon] = useState(ngayHomNay);
  const [danhSachSuatTrong, setDanhSachSuatTrong] = useState([]);
  const [suatDaChọn, setSuatDaChọn] = useState([]);
  const [dangTai, setDangTai] = useState(false);
  const [thongBaoLoi, setThongBaoLoi] = useState('');
  const [dangDat, setDangDat] = useState(false);
  const [lanTai, setLanTai] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    async function taiLichTrong() {
      setDangTai(true);
      setThongBaoLoi('');
      setDanhSachSuatTrong([]);
      setSuatDaChọn([]);
      try {
        const response = await api.get('/lich-san/trong', {
          params: { sanId, ngay: ngayChon }, signal: controller.signal,
        });
        if (!controller.signal.aborted) setDanhSachSuatTrong(response.data);
      } catch (error) {
        if (!controller.signal.aborted) setThongBaoLoi(layThongBaoLoi(error, 'Không thể tải khung giờ trống.'));
      } finally {
        if (!controller.signal.aborted) setDangTai(false);
      }
    }
    void taiLichTrong();
    return () => controller.abort();
  }, [ngayChon, sanId, lanTai]);

  const toggleChonSuat = (lichId) => {
    if (suatDaChọn.includes(lichId)) {
      setSuatDaChọn(suatDaChọn.filter(id => id !== lichId));
    } else {
      setSuatDaChọn([...suatDaChọn, lichId]);
    }
  };

  const tongTien = danhSachSuatTrong
    .filter(suat => suatDaChọn.includes(suat.id))
      .reduce((tong, suat) => tong + Number(suat.gia || 0), 0);

  const handleDatSan = async () => {
    if (dangDat || dangTai) return;
    const token = localStorage.getItem('token');
    if (!token) {
      alert('Vui lòng đăng nhập trước khi đặt sân!');
      navigate('/dang-nhap');
      return;
    }

    if (suatDaChọn.length === 0) {
      alert('Vui lòng chọn ít nhất một khung giờ!');
      return;
    }

    setDangDat(true);
    try {
      // Backend tự động lấy thông tin người dùng từ JWT Token (Principal)
      const response = await api.post('/dat-san', {
        danhSachLichSanIds: suatDaChọn
      });

      alert('Đặt sân thành công! Mã đơn: ' + response.data.maDat);
      navigate('/lich-su');
    } catch (error) {
      alert(layThongBaoLoi(error, 'Không thể đặt sân. Vui lòng thử lại.'));
      if (error.response?.status === 409) setLanTai(value => value + 1);
    } finally {
      setDangDat(false);
    }
  };

  return (
    <div className="flex min-h-screen bg-[#f3f4f6]">
      <aside className="w-64 bg-[#0f172a] text-white flex flex-col hidden md:flex">
        <div className="p-5 flex items-center space-x-3 border-b border-gray-800">
          <div className="bg-emerald-500 p-2 rounded-lg font-black text-xl">🏸</div>
          <span className="font-bold text-lg tracking-wide">SÂN CẦU LÔNG</span>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-1">
          <a href="/" className="flex items-center space-x-3 px-4 py-3 text-gray-400 hover:bg-gray-800 hover:text-white rounded-xl font-medium transition">
            <span>🏠</span>
            <span>Trang Chủ</span>
          </a>
          <a href="/lich-su" className="flex items-center space-x-3 px-4 py-3 text-gray-400 hover:bg-gray-800 hover:text-white rounded-xl font-medium transition">
            <span>📅</span>
            <span>Sân Đã Đặt</span>
          </a>
        </nav>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="bg-white border-b px-6 py-4 flex items-center justify-between sticky top-0 z-20 shadow-xs">
          <h1 className="text-lg font-bold text-gray-800">Chọn Khung Giờ & Đặt Sân</h1>
          <button 
            onClick={() => navigate('/')}
            className="px-4 py-1.5 text-xs font-semibold bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg transition"
          >
            Quay lại Trang Chủ
          </button>
        </header>

        <main className="p-6 max-w-4xl mx-auto w-full space-y-6">
          <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100 flex items-center justify-between">
            <div>
              <label className="block text-xs font-semibold text-gray-500 mb-1">Chọn ngày chơi</label>
              <input 
                type="date" 
                min={ngayHomNay()}
                disabled={dangDat}
                value={ngayChon}
                onChange={(e) => {
                  setNgayChon(e.target.value);
                  setSuatDaChọn([]); // Xóa các suất đã chọn khi đổi ngày
                }}
                className="bg-gray-50 border border-gray-200 rounded-xl px-4 py-2 text-sm font-semibold focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              />
            </div>
            <div className="text-right">
              <span className="text-xs text-gray-400 block">Đã chọn:</span>
              <span className="font-bold text-emerald-600 text-sm">{suatDaChọn.length} suất lịch</span>
            </div>
          </div>

          {thongBaoLoi && (
            <div className="p-4 text-sm text-red-700 bg-red-100 rounded-xl">
              {thongBaoLoi}
            </div>
          )}

          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
            <h3 className="font-bold text-gray-900 mb-4 text-base">Danh Sách Khung Giờ Trống</h3>

            {dangTai ? (
              <div className="text-center py-10 text-gray-400 text-sm">Đang tải lịch trống...</div>
            ) : danhSachSuatTrong.length === 0 ? (
              <div className="text-center py-10 text-gray-400 text-sm">Không có suất lịch trống nào trong ngày này.</div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                {danhSachSuatTrong.map((suat) => {
                  const daChon = suatDaChọn.includes(suat.id);
                  // Đọc giờ từ quan hệ khungGio hoặc trực tiếp tùy theo entity của bạn
                  const gioBatDau = suat.khungGio?.gioBatDau || suat.gioBatDau || '00:00';
                  const gioKetThuc = suat.khungGio?.gioKetThuc || suat.gioKetThuc || '00:00';

                  return (
                    <div 
                      key={suat.id}
                      onClick={() => { if (!dangDat) toggleChonSuat(suat.id); }}
                      className={`cursor-pointer p-4 rounded-xl border transition flex flex-col justify-between ${
                        daChon 
                          ? 'bg-emerald-50 border-emerald-500 shadow-sm' 
                          : 'bg-white border-gray-200 hover:border-gray-300'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <span className="font-bold text-gray-800 text-sm">⏰ {gioBatDau} - {gioKetThuc}</span>
                        <input 
                          type="checkbox" 
                          checked={daChon} 
                          onChange={() => {}} 
                          className="w-4 h-4 text-emerald-600 rounded focus:ring-emerald-500"
                        />
                      </div>
                      <div className="flex items-center justify-between mt-2 pt-2 border-t border-gray-100 text-xs">
                        <span className="text-gray-500">Giá thuê:</span>
                        <span className="font-bold text-emerald-700">{tienViet(suat.gia)}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          <div className="bg-white p-5 rounded-2xl shadow-md border border-emerald-100 flex items-center justify-between">
            <div>
              <span className="text-xs text-gray-500 block">Tổng cộng thanh toán:</span>
              <span className="text-xl font-black text-emerald-600">{tienViet(tongTien)}</span>
            </div>
            <button 
              onClick={handleDatSan}
              disabled={suatDaChọn.length === 0 || dangDat || dangTai}
              className={`px-6 py-3 rounded-xl font-bold text-sm shadow-md transition ${
                suatDaChọn.length === 0 
                  ? 'bg-gray-200 text-gray-400 cursor-not-allowed' 
                  : 'bg-amber-500 hover:bg-amber-600 text-white'
              }`}
            >
              {dangDat ? 'Đang đặt sân...' : 'Xác Nhận Đặt Sân ➔'}
            </button>
          </div>
        </main>
      </div>
    </div>
  );
}
