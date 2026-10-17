import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';
import { tienViet } from '../utils/hienThi';

export default function LichSuDatSan() {
  const navigate = useNavigate();
  const [danhSachDatSan, setDanhSachDatSan] = useState([]);
  const [dangTai, setDangTai] = useState(true);
  const [thongBaoLoi, setThongBaoLoi] = useState('');
  const [lyDoHuy, setLyDoHuy] = useState('');
  const [dangHuyId, setDangHuyId] = useState(null);
  const [dangGui, setDangGui] = useState(false);
  const [lanTai, setLanTai] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    async function taiLichSuDatSan() {
      setDangTai(true);
      setThongBaoLoi('');
      try {
        const response = await api.get('/dat-san/lich-su', { signal: controller.signal });
        if (!controller.signal.aborted) setDanhSachDatSan(response.data);
      } catch (error) {
        if (!controller.signal.aborted) setThongBaoLoi(layThongBaoLoi(error, 'Không thể tải lịch sử đặt sân.'));
      } finally {
        if (!controller.signal.aborted) setDangTai(false);
      }
    }
    void taiLichSuDatSan();
    return () => controller.abort();
  }, [lanTai]);

  const handleHuyDon = async (id) => {
    if (dangGui) return;
    if (!window.confirm('Bạn có chắc chắn muốn hủy đơn đặt sân này không?')) return;
    setDangGui(true);
    try {
      await api.put(`/dat-san/${id}/huy`, { lyDoHuy: lyDoHuy.trim() || 'Khách hàng hủy' });
      alert('Hủy đơn đặt sân thành công!');
      setDangHuyId(null);
      setLyDoHuy('');
      setLanTai(value => value + 1);
    } catch (error) {
      alert(layThongBaoLoi(error, 'Không thể hủy đơn. Vui lòng thử lại.'));
      if (error.response?.status === 409) setLanTai(value => value + 1);
    } finally {
      setDangGui(false);
    }
  };

  return (
    <div className="flex min-h-screen bg-[#f3f4f6]">
      {/* Sidebar bên trái */}
      <aside className="w-64 bg-[#0f172a] text-white flex flex-col hidden md:flex">
        <div className="p-5 flex items-center space-x-3 border-b border-gray-800">
          <div className="bg-emerald-500 p-2 rounded-lg font-black text-xl">🏸</div>
          <span className="font-bold text-lg tracking-wide">SÂN CẦU LÔNG</span>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-1">
          <button 
            onClick={() => navigate('/')} 
            className="w-full flex items-center space-x-3 px-4 py-3 text-gray-400 hover:bg-gray-800 hover:text-white rounded-xl font-medium transition text-left"
          >
            <span>🏠</span>
            <span>Trang Chủ</span>
          </button>
          <button 
            onClick={() => navigate('/lich-su')} 
            className="w-full flex items-center space-x-3 px-4 py-3 bg-emerald-600 text-white rounded-xl font-medium shadow-sm text-left"
          >
            <span>📅</span>
            <span>Sân Đã Đặt</span>
          </button>
        </nav>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="bg-white border-b px-6 py-4 flex items-center justify-between sticky top-0 z-20 shadow-xs">
          <h1 className="text-lg font-bold text-gray-800">Lịch Sử Đặt Sân Của Tôi</h1>
          <button 
            onClick={() => navigate('/')}
            className="px-4 py-1.5 text-xs font-semibold bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg transition"
          >
            Quay lại Trang Chủ
          </button>
        </header>

        <main className="p-6 max-w-5xl mx-auto w-full space-y-6">
          {thongBaoLoi && (
            <div className="p-4 text-sm text-red-700 bg-red-100 rounded-xl">
              {thongBaoLoi}
            </div>
          )}

          {dangTai ? (
            <div className="text-center py-16 text-gray-400 text-sm">Đang tải lịch sử đơn hàng...</div>
          ) : danhSachDatSan.length === 0 ? (
            <div className="bg-white p-12 rounded-2xl text-center shadow-sm border border-gray-100">
              <span className="text-4xl block mb-3">📭</span>
              <p className="text-gray-600 font-semibold mb-2">Bạn chưa có đơn đặt sân nào.</p>
              <p className="text-gray-400 text-xs mb-6">Hãy chọn sân và đặt lịch ngay để trải nghiệm dịch vụ!</p>
              <button 
                onClick={() => navigate('/')}
                className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs shadow transition"
              >
                Đặt Sân Ngay
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {danhSachDatSan.map((don) => {
                const laDaHuy = don.trangThai === 'DA_HUY';
                return (
                  <div key={don.id} className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                    <div className="space-y-2 flex-1">
                      <div className="flex items-center space-x-3">
                        <span className="font-bold text-gray-900 text-base">Đơn hàng #{don.id}</span>
                        <span className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                          laDaHuy 
                            ? 'bg-red-100 text-red-700' 
                            : 'bg-emerald-100 text-emerald-700'
                        }`}>
                          {{ CHO_XAC_NHAN: 'Chờ xác nhận', DA_XAC_NHAN: 'Đã xác nhận', DA_HOAN_THANH: 'Đã hoàn thành', DA_HUY: 'Đã hủy' }[don.trangThai] || don.trangThai}
                        </span>
                      </div>
                      
                      <div className="text-xs text-gray-500 space-y-1">
                        <p>📅 Ngày tạo đơn: <span className="font-semibold text-gray-700">{don.ngayTao ? new Date(don.ngayTao).toLocaleString() : 'N/A'}</span></p>
                        {don.lyDoHuy && <p className="text-red-500">❌ Lý do hủy: {don.lyDoHuy}</p>}
                      </div>

                      {/* Chi tiết các suất lịch trong đơn */}
                      <div className="mt-3 bg-gray-50 p-3 rounded-xl border border-gray-100">
                        <p className="text-xs font-bold text-gray-700 mb-2">🏸 Chi tiết khung giờ đã đặt:</p>
                        <div className="space-y-1.5">
                          {don.chiTietDatSans && don.chiTietDatSans.map((chiTiet, idx) => {
                            const san = chiTiet.lichSan?.san;
                            const khungGio = chiTiet.lichSan?.khungGio;
                            const ngayChoi = chiTiet.lichSan?.ngayChoi;
                            return (
                              <div key={idx} className="flex flex-wrap items-center justify-between text-xs bg-white p-2 rounded-lg border border-gray-200">
                                <span className="font-semibold text-gray-800">{san?.tenSan || 'Sân cầu lông'} ({san?.loaiSan || 'Tiêu chuẩn'})</span>
                                <span className="text-gray-600">📅 {ngayChoi} | ⏰ {khungGio?.gioBatDau} - {khungGio?.gioKetThuc}</span>
                                <span className="font-bold text-emerald-600">{tienViet(chiTiet.donGia)}</span>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-col items-end justify-between w-full md:w-auto border-t md:border-t-0 pt-4 md:pt-0 border-gray-100 gap-3">
                      <div className="text-right">
                        <span className="text-xs text-gray-400 block">Tổng tiền thanh toán</span>
                        <span className="text-lg font-black text-emerald-600">{tienViet(don.tongTien)}</span>
                      </div>

                      {don.coTheHuy && (
                        <div>
                          {dangHuyId === don.id ? (
                            <div className="space-y-2 w-full md:w-60">
                              <input 
                                type="text"
                                maxLength={500}
                                placeholder="Nhập lý do hủy..."
                                value={lyDoHuy}
                                onChange={(e) => setLyDoHuy(e.target.value)}
                                className="w-full text-xs px-3 py-1.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:outline-none"
                              />
                              <div className="flex space-x-2">
                                <button 
                                  onClick={() => handleHuyDon(don.id)}
                                  disabled={dangGui}
                                  className="flex-1 bg-red-600 hover:bg-red-700 text-white font-bold text-xs py-1.5 rounded-lg transition"
                                >
                                 Xác nhận hủy
                                </button>
                                <button 
                                  onClick={() => setDangHuyId(null)}
                                  className="bg-gray-200 hover:bg-gray-300 text-gray-700 font-bold text-xs px-3 py-1.5 rounded-lg transition"
                                >
                                 Đóng
                                </button>
                              </div>
                            </div>
                          ) : (
                            <button 
                              onClick={() => setDangHuyId(don.id)}
                              className="px-4 py-2 bg-red-50 hover:bg-red-100 text-red-600 font-bold text-xs rounded-xl border border-red-200 transition"
                            >
                              Hủy Đơn Này
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
