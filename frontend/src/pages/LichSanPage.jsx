import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import useDuLieu from '../hooks/useDuLieu';
import { usePhienDangNhap } from '../context/PhienContext';
import { ngayHomNay, tienViet } from '../utils/hienThi';
import api, { layThongBaoLoi } from '../services/api';

export default function LichSanPage() {
  const { nguoiDung } = usePhienDangNhap();
  const admin = nguoiDung?.vaiTro === 'ADMIN';
  const [query] = useSearchParams(), navigate = useNavigate();
  const [day, setDay] = useState(() => admin ? query.get('ngay') || ngayHomNay() : ngayHomNay());
  const { data, error, loading, reload } = useDuLieu('/quan-tri/lich-san', { ngay: day });
  const [selected, setSelected] = useState([]);
  const [selectedCourt, setSelectedCourt] = useState('ALL'); // 'ALL' hoặc sanId

  const [search, setSearch] = useState(''), [customerId, setCustomerId] = useState('');
  const customers = useDuLieu('/quan-tri/tim-khach', { tim: search });
  const [edit, setEdit] = useState(null), [busy, setBusy] = useState(false), [actionError, setActionError] = useState('');
  const [now, setNow] = useState(Date.now);

  useEffect(() => { const timer = setInterval(() => setNow(Date.now()), 30000); return () => clearInterval(timer); }, []);

  const courts = [...new Map((data || []).map(row => [row.sanId, { id: row.sanId, name: row.tenSan }])).values()];
  const times = [...new Set((data || []).map(row => row.gioBatDau))];
  const byCell = new Map((data || []).map(row => [row.sanId + '_' + row.gioBatDau, row]));

  function future(row) { return new Date(row.ngayChoi + 'T' + row.gioBatDau + '+07:00').getTime() > now; }
  
  function toggle(id) {
    setSelected(ids => ids.includes(id) ? ids.filter(value => value !== id) : [...ids, id]);
  }

  async function saveSlot(e) {
    e.preventDefault(); if (busy) return; setBusy(true); setActionError('');
    try {
      await api.put('/quan-tri/lich-san/' + edit.id, { gia: Number(edit.gia), moDat: !!edit.moDat });
      setSelected(ids => ids.filter(id => id !== edit.id)); setEdit(null); reload();
    } catch (err) { setActionError(layThongBaoLoi(err)); } finally { setBusy(false); }
  }

  async function book(e) {
    e.preventDefault(); if (busy) return; setBusy(true); setActionError('');
    try {
      const response = await api.post('/quan-tri/dat-tai-quay', { nguoiDungId: Number(customerId), danhSachLichSanIds: selected });
      navigate('/quan-tri/don-dat?don=' + response.data.don.id);
    } catch (err) {
      setActionError(layThongBaoLoi(err));
      if (err.response?.status === 409) { setSelected([]); reload(); }
    } finally { setBusy(false); }
  }

  const selectedRows = (data || []).filter(row => selected.includes(row.id));
  const total = selectedRows.reduce((sum, row) => sum + Number(row.gia), 0);

  const displayedCourts = selectedCourt === 'ALL' ? courts : courts.filter(c => c.id === Number(selectedCourt));

  return (
    <div className="space-y-6 text-slate-800 font-sans antialiased pb-24">
      {/* HEADER BẢNG ĐIỀU KHIỂN & LỌC NGÀY */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-6 rounded-2xl bg-white border border-slate-200 shadow-sm">
        <div className="space-y-1">
          <div className="flex items-center gap-2.5">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 text-xl font-bold">
              🗓️
            </span>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-slate-900">
                Lịch Sân & Đặt Tại Quầy
              </h1>
              <p className="text-xs text-slate-500">
                Click vào suất trống để chọn đặt nhanh cho khách hàng tại quầy
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-xs">
            <span className="text-slate-500 font-medium">📅 Ngày chơi:</span>
            <input
              type="date"
              disabled={!admin || busy}
              value={day}
              onChange={e => { setDay(e.target.value); setSelected([]); }}
              className="bg-transparent text-slate-900 font-semibold focus:outline-none cursor-pointer"
            />
          </div>
          <button onClick={reload} disabled={busy} className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs transition shadow-sm">
            🔄 Tải lại lịch
          </button>
        </div>
      </div>

      {/* CHÚ THÍCH TRẠNG THÁI (LEGEND) & BỘ LỌC CHỌN SÂN */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm text-xs">
        {/* Tab Lọc Sân */}
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-slate-400 font-medium mr-1">Lọc sân:</span>
          <button
            onClick={() => setSelectedCourt('ALL')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${selectedCourt === 'ALL' ? 'bg-indigo-600 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
          >
            Tất cả sân ({courts.length})
          </button>
          {courts.map(c => (
            <button
              key={c.id}
              onClick={() => setSelectedCourt(c.id)}
              className={`px-3 py-1.5 rounded-lg font-semibold transition ${selectedCourt === c.id ? 'bg-indigo-600 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
            >
              {c.name}
            </button>
          ))}
        </div>

        {/* Chú thích màu sắc */}
        <div className="flex flex-wrap items-center gap-3 text-[11px] font-medium text-slate-600 border-t sm:border-t-0 pt-2 sm:pt-0">
          <span className="flex items-center gap-1.5"><span className="h-3 w-3 rounded bg-emerald-100 border border-emerald-300" /> Trống</span>
          <span className="flex items-center gap-1.5"><span className="h-3 w-3 rounded bg-indigo-600" /> Đã chọn</span>
          <span className="flex items-center gap-1.5"><span className="h-3 w-3 rounded bg-rose-100 border border-rose-300" /> Đã đặt</span>
          <span className="flex items-center gap-1.5"><span className="h-3 w-3 rounded bg-slate-100 border border-slate-300" /> Đóng/Qua giờ</span>
        </div>
      </div>

      {/* THÔNG BÁO LỖI */}
      {(error || actionError || customers.error) && (
        <div role="alert" className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs font-medium">
          ⚠️ {error || actionError || customers.error}
        </div>
      )}

      {loading && (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200 text-xs text-slate-500">
          Đang cập nhật lịch sân...
        </div>
      )}

      {!loading && !data?.length && (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200 text-xs text-slate-500">
          Chưa có lịch cho ngày này. Hệ thống tự động mở lịch cho 7 ngày tính từ hôm nay.
        </div>
      )}

      {/* MA TRẬN LỊCH SÂN TỎA SÁNG GỌN GÀNG */}
      {!!data?.length && (
        <div className="rounded-2xl bg-white border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead className="bg-slate-50 text-slate-600 font-bold uppercase tracking-wider border-b border-slate-200">
                <tr>
                  <th className="p-3.5 w-20 text-center border-r border-slate-200">Khung giờ</th>
                  {displayedCourts.map(c => (
                    <th key={c.id} className="p-3.5 text-center border-r border-slate-200 min-w-[120px]">
                      {c.name}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {times.map(time => (
                  <tr key={time} className="hover:bg-slate-50/50">
                    <td className="p-2 text-center font-bold text-slate-500 bg-slate-50/80 border-r border-slate-200 whitespace-nowrap">
                      {time.slice(0, 5)}
                    </td>
                    {displayedCourts.map(c => {
                      const row = byCell.get(c.id + '_' + time);
                      if (!row) return <td key={c.id} className="p-1.5 text-center text-slate-300 border-r border-slate-100">—</td>;
                      
                      const available = !row.donId && !!row.moDat && !!row.sanHoatDong && future(row);
                      const isSelected = selected.includes(row.id);

                      // TRƯỜNG HỢP 1: ĐÃ ĐƯỢC ĐẶT
                      if (row.donId) {
                        return (
                          <td key={c.id} className="p-1.5 border-r border-slate-100">
                            <Link
                              to={'/quan-tri/don-dat?don=' + row.donId}
                              className="block p-2 rounded-xl bg-rose-50 hover:bg-rose-100 border border-rose-200 text-rose-800 text-[11px] font-semibold text-center transition truncate"
                              title={`Đơn #${row.donId} - Khách: ${row.khachHang}`}
                            >
                              #{row.donId} · {row.khachHang}
                            </Link>
                          </td>
                        );
                      }

                      // TRƯỜNG HỢP 2: SUẤT TRỐNG ĐANG MỞ
                      if (available) {
                        return (
                          <td key={c.id} className="p-1.5 border-r border-slate-100">
                            <div className="relative group">
                              <button
                                type="button"
                                disabled={busy}
                                onClick={() => toggle(row.id)}
                                className={`w-full p-2 rounded-xl border text-center transition flex flex-col items-center justify-center gap-0.5 ${
                                  isSelected
                                    ? 'bg-indigo-600 text-white border-indigo-600 shadow-sm font-bold scale-[0.98]'
                                    : 'bg-emerald-50/60 hover:bg-emerald-100 border-emerald-200/80 text-emerald-900 font-semibold'
                                }`}
                              >
                                <span className="text-[11px]">{tienViet(row.gia)}</span>
                                <span className="text-[10px] opacity-80">{isSelected ? '✓ Đã chọn' : 'Trống'}</span>
                              </button>

                              {/* Nút sửa giá nhanh dành cho Admin khi hover */}
                              {admin && (
                                <button
                                  type="button"
                                  onClick={(e) => { e.stopPropagation(); setEdit({ ...row, moDat: !!row.moDat }); setActionError(''); }}
                                  className="absolute top-1 right-1 hidden group-hover:block p-1 bg-white rounded-md border shadow text-[9px] text-slate-600 hover:text-indigo-600"
                                  title="Chỉnh sửa giá/đóng suất"
                                >
                                  ✏️
                                </button>
                              )}
                            </div>
                          </td>
                        );
                      }

                      // TRƯỜNG HỢP 3: ĐÓNG ĐẶT HOẶC ĐÃ QUA GIỜ
                      return (
                        <td key={c.id} className="p-1.5 border-r border-slate-100">
                          <div
                            onClick={() => admin && future(row) && setEdit({ ...row, moDat: !!row.moDat })}
                            className={`p-2 rounded-xl border text-center text-[11px] text-slate-400 bg-slate-50 border-slate-200/60 ${admin && future(row) ? 'cursor-pointer hover:border-slate-300' : 'cursor-not-allowed'}`}
                          >
                            <span>{future(row) ? 'Đóng' : 'Đã qua'}</span>
                          </div>
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* THANH THÔNG TIN ĐẶT SÂN NỔI PHÍA DƯỚI (FLOATING BAR) */}
      <div className={`fixed bottom-4 left-4 right-4 md:left-72 z-40 transition-all duration-300 transform ${selected.length > 0 ? 'translate-y-0 opacity-100' : 'translate-y-20 opacity-0 pointer-events-none'}`}>
        <form onSubmit={book} className="mx-auto max-w-4xl p-4 bg-slate-900 text-white rounded-2xl shadow-2xl border border-slate-800 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-4 w-full md:w-auto">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-600 font-bold text-lg">
              🛒
            </span>
            <div>
              <p className="text-xs text-slate-400">Đã chọn <strong className="text-white">{selected.length}</strong> suất sân</p>
              <p className="text-base font-extrabold text-emerald-400">Tạm tính: {tienViet(total)}</p>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row items-center gap-3 w-full md:w-auto">
            {/* Khung tìm khách */}
            <div className="relative w-full sm:w-64">
              <input
                className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                placeholder="Tìm khách (tên/email/SĐT)..."
                value={search}
                onChange={e => { setSearch(e.target.value); setCustomerId(''); }}
              />
            </div>

            {/* Select chọn khách */}
            <select
              className="w-full sm:w-56 px-3 py-2 bg-slate-800 border border-slate-700 rounded-xl text-xs text-white focus:outline-none"
              required
              value={customerId}
              onChange={e => setCustomerId(e.target.value)}
            >
              <option value="">-- Chọn khách hàng --</option>
              {(customers.data || []).map(c => (
                <option key={c.id} value={c.id}>{c.hoTen} ({c.soDienThoai || c.email})</option>
              ))}
            </select>

            <div className="flex gap-2 w-full sm:w-auto">
              <button
                disabled={busy || loading || !selected.length || !customerId}
                className="flex-1 sm:flex-none px-4 py-2 bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-slate-950 font-bold text-xs rounded-xl transition whitespace-nowrap"
              >
                {busy ? 'Đang tạo...' : 'Tạo đơn ngay'}
              </button>
              <button
                type="button"
                onClick={() => setSelected([])}
                className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold text-xs rounded-xl transition"
              >
                Hủy
              </button>
            </div>
          </div>
        </form>
      </div>

      {/* MODAL SỬA GIÁ & TRẠNG THÁI SUẤT */}
      {edit && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <form onSubmit={saveSlot} className="w-full max-w-md bg-white rounded-2xl p-6 shadow-2xl space-y-4">
            <h2 className="font-bold text-base text-slate-900 border-b pb-3">
              ⚙️ Cấu hình suất chơi: {edit.tenSan}
            </h2>
            <p className="text-xs text-slate-500">
              📅 Ngày {edit.ngayChoi} | ⏰ Khung giờ {edit.gioBatDau}
            </p>

            <label className="block text-xs font-semibold text-slate-700">Giá suất (VNĐ)
              <input
                type="number"
                min="0"
                step="1000"
                required
                value={edit.gia}
                onChange={e => setEdit({ ...edit, gia: e.target.value })}
                className="mt-1 w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm"
              />
            </label>

            <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 pt-1">
              <input
                type="checkbox"
                checked={edit.moDat}
                onChange={e => setEdit({ ...edit, moDat: e.target.checked })}
                className="h-4 w-4 text-indigo-600 rounded"
              />
              Mở cho phép khách hàng đặt suất này
            </label>

            {actionError && <p className="text-xs text-red-600">{actionError}</p>}

            <div className="flex justify-end gap-2 pt-4 border-t">
              <button
                type="button"
                disabled={busy}
                onClick={() => setEdit(null)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-xs font-semibold text-slate-700 rounded-xl transition"
              >
                Đóng
              </button>
              <button
                disabled={busy}
                className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-xs font-bold text-white rounded-xl transition shadow"
              >
                Lưu cấu hình
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}