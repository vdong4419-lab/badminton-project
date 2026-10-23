import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';
import { usePhienDangNhap } from '../context/PhienContext';
import { tienViet, thoiGianViet, ngayHomNay, tenTrangThaiDon, tenTrangThaiTien } from '../utils/hienThi';

const filters = [['', 'Tất cả đơn'], ['CHO_XAC_NHAN', 'Chờ xác nhận'], ['DA_XAC_NHAN', 'Đã xác nhận'], ['CHO_HOAN_TIEN', 'Chờ hoàn tiền'], ['DA_HOAN_THANH', 'Hoàn thành'], ['DA_HUY', 'Đã hủy']];
const labels = { XAC_NHAN: 'Xác nhận đơn', THANH_TOAN: 'Ghi nhận thu tiền', HUY: 'Hủy đơn', HOAN_TIEN: 'Xác nhận đã hoàn tiền', CHECK_IN: 'Khách đã đến', HOAN_THANH: 'Hoàn thành đơn' };
const actionUrls = { XAC_NHAN: 'xac-nhan', THANH_TOAN: 'thanh-toan', HUY: 'huy', HOAN_TIEN: 'hoan-tien', CHECK_IN: 'check-in', HOAN_THANH: 'hoan-thanh' };

export default function DonDatPage() {
  const { nguoiDung } = usePhienDangNhap();
  
  // States cho Quản lý đơn
  const [filter, setFilter] = useState('');
  const [page, setPage] = useState(0);
  const [reload, setReload] = useState(0);
  const [data, setData] = useState({ items: [], totalElements: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query] = useSearchParams();
  const [selectedId, setSelectedId] = useState(() => Number(query.get('don')) || null);
  const [day, setDay] = useState('');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [action, setAction] = useState('');
  const [method, setMethod] = useState('TIEN_MAT');
  const [reference, setReference] = useState('');
  const [note, setNote] = useState('');
  const [acknowledged, setAcknowledged] = useState(false);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState('');

  // Tải danh sách đơn
  useEffect(() => {

    const controller = new AbortController();
    async function load() {
      setLoading(true); setError('');
      try {
        const response = await api.get('/quan-tri/don-dat', {
          params: { page, ngay: day || undefined, tim: search, trangThai: filter && filter !== 'CHO_HOAN_TIEN' ? filter : undefined, choHoanTien: filter === 'CHO_HOAN_TIEN' },
          signal: controller.signal,
        });
        if (!controller.signal.aborted) setData(response.data);
      } catch (err) { if (!controller.signal.aborted) setError(layThongBaoLoi(err)); }
      finally { if (!controller.signal.aborted) setLoading(false); }
    }
    void load();
    return () => controller.abort();
  }, [filter, page, reload, day, search]);

  // Tải chi tiết đơn
  useEffect(() => {
    if (selectedId === null) return undefined;
    const controller = new AbortController();
    async function load() {
      setDetailLoading(true); setSelected(null);
      try {
        const response = await api.get('/quan-tri/don-dat/' + selectedId, { signal: controller.signal });
        if (!controller.signal.aborted) setSelected(response.data);
      } catch (err) { if (!controller.signal.aborted) setError(layThongBaoLoi(err)); }
      finally { if (!controller.signal.aborted) setDetailLoading(false); }
    }
    void load();
    return () => controller.abort();
  }, [selectedId, reload]);

  function openAction(value) {
    setAction(value); setReference(''); setNote(''); setAcknowledged(false); setNotice(''); setError('');
    setMethod(value === 'THANH_TOAN' && selected?.don.chuyenKhoan?.daBaoLuc ? 'CHUYEN_KHOAN' : 'TIEN_MAT');
  }

  async function submit(event) {
    event.preventDefault();
    if (busy || !selected || !selected.hanhDong.includes(action)) return;
    setBusy(true); setError(''); setNotice('');
    try {
      let body;
      if (action === 'THANH_TOAN') body = { phuongThuc: method, maThamChieu: reference.trim() || null };
      if (action === 'HUY') body = { lyDoHuy: note.trim() };
      if (action === 'HOAN_TIEN') body = { maBienNhan: reference.trim(), ghiChu: note.trim() };
      const verb = ['THANH_TOAN', 'HOAN_TIEN'].includes(action) ? 'post' : 'put';
      await api[verb]('/quan-tri/don-dat/' + selected.don.id + '/' + actionUrls[action], body);
      setNotice('Đã cập nhật đơn thành công.'); setAction(''); setReload(value => value + 1);
    } catch (err) {
      setError(layThongBaoLoi(err));
      if (err.response?.status === 409) { setAction(''); setReload(value => value + 1); }
    } finally { setBusy(false); }
  }

  const booking = selected?.don;
  const money = booking?.thanhToan;
  return <div className="space-y-4">
    <h1 className="text-2xl font-bold">Đơn đặt sân</h1>
    <div className="flex flex-wrap gap-3 rounded-2xl bg-white p-4 border">
      <label>Tìm mã đơn, tên, điện thoại<input className="ql-input" maxLength={100} value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} /></label>
      <label>Ngày chơi<input className="ql-input" type="date" disabled={nguoiDung?.vaiTro === 'STAFF'} value={nguoiDung?.vaiTro === 'STAFF' ? ngayHomNay() : day} onChange={e => { setDay(e.target.value); setPage(0); }} /></label>
      {nguoiDung?.vaiTro === 'ADMIN' && <button className="ql-button" onClick={() => setDay('')}>Mọi ngày</button>}
    </div>
    {error && <p role="alert" className="ql-error">{error}</p>}
    {notice && <p role="status" className="ql-success">{notice}</p>}          <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
              <div className="flex flex-wrap gap-2">
                {filters.map(([value, title]) => (
                  <button key={value} disabled={busy} onClick={() => { setFilter(value); setPage(0); }}
                    className={'rounded-full px-4 py-2 text-xs font-semibold transition ' + (filter === value ? 'bg-indigo-600 text-white shadow' : 'bg-slate-100 text-slate-600 hover:bg-slate-200')}>
                    {title}
                  </button>
                ))}
              </div>
              <button disabled={busy} className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold hover:bg-slate-50 transition" onClick={() => setReload(value => value + 1)}>
                🔄 Tải lại dữ liệu
              </button>
            </div>

            <div className="grid gap-6 lg:grid-cols-12">
              {/* Danh sách đơn */}
              <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm lg:col-span-5">
                <div className="border-b bg-slate-50 p-4 text-xs font-bold uppercase text-slate-600 flex justify-between items-center">
                  <span>Danh sách đơn đặt</span>
                  <span>Trang {page + 1}/{Math.max(data.totalPages, 1)}</span>
                </div>
                {loading ? <p className="p-6 text-sm text-slate-500 text-center">Đang tải dữ liệu đơn...</p> : data.items.length === 0 ? <p className="p-6 text-sm text-slate-500 text-center">Không có đơn đặt sân phù hợp.</p> :
                  <ul className="divide-y divide-slate-100 max-h-[600px] overflow-y-auto">{data.items.map(item => (
                    <li key={item.don.id}><button disabled={busy} onClick={() => { setSelectedId(item.don.id); setAction(''); setError(''); }}
                      className={'w-full space-y-2 p-4 text-left transition hover:bg-indigo-50/60 ' + (selectedId === item.don.id ? 'bg-indigo-50 border-l-4 border-indigo-600' : '')}>
                      <div className="flex items-center justify-between gap-2">
                        <strong className="text-sm text-slate-900">#{item.don.id} · {item.khach.hoTen}</strong>
                        <span className="text-sm font-bold text-indigo-600">{tienViet(item.don.tongTien)}</span>
                      </div>
                      <div className="flex flex-wrap justify-between gap-2 text-xs text-slate-500 font-medium">
                        <span className="px-2 py-0.5 rounded bg-slate-100">{tenTrangThaiDon(item.don.trangThai)}</span>
                        <span>{tenTrangThaiTien(item.don.thanhToan)}</span>
                      </div>
                      {item.don.chuyenKhoan?.daBaoLuc && item.don.thanhToan?.trangThai === 'CHUA_THANH_TOAN' && item.don.trangThai !== 'DA_HUY' && <p className="text-xs font-semibold text-amber-800">Khách đã báo chuyển khoản · Cần đối chiếu</p>}
                    </button></li>
                  ))}</ul>}
                <div className="flex justify-between border-t bg-slate-50 p-4 text-sm">
                  <button disabled={loading || busy || page === 0} onClick={() => setPage(value => value - 1)} className="rounded-xl border bg-white px-4 py-2 text-xs font-semibold disabled:opacity-40 hover:bg-slate-100 transition">Trang trước</button>
                  <button disabled={loading || busy || page + 1 >= data.totalPages} onClick={() => setPage(value => value + 1)} className="rounded-xl border bg-white px-4 py-2 text-xs font-semibold disabled:opacity-40 hover:bg-slate-100 transition">Trang sau</button>
                </div>
              </section>

              {/* Chi tiết đơn & Thao tác xử lý */}
              <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm lg:col-span-7">
                {detailLoading ? <p className="text-sm text-slate-500 text-center py-10">Đang tải thông tin chi tiết đơn...</p> : !selected ? (
                  <div className="text-center py-16 text-slate-400">
                    <p className="text-4xl mb-2">👈</p>
                    <p className="text-sm font-medium">Chọn một đơn đặt sân bên danh sách để xem chi tiết và thực hiện nghiệp vụ.</p>
                  </div>
                ) : (
                  <div className="space-y-6">
                    <div className="border-b pb-4 flex justify-between items-start">
                      <div>
                        <h2 className="text-xl font-bold text-slate-900">Chi tiết đơn hàng #{booking.id}</h2>
                        <p className="text-xs text-slate-400 font-mono mt-1">Mã định danh: {booking.maDat}</p>
                      </div>
                      <div className="text-right">
                        <span className="inline-block px-3 py-1 rounded-full text-xs font-bold bg-indigo-100 text-indigo-800">{tenTrangThaiDon(booking.trangThai)}</span>
                        <p className="text-xs text-slate-500 mt-1">{tenTrangThaiTien(money)}</p>
                      </div>
                    </div>

                    {booking.hetHanGiuChoLuc && (
                      <div className="rounded-xl bg-amber-50 border border-amber-200 p-3 text-xs text-amber-800 font-medium">
                        ⚠️ Hạn xác nhận giữ chỗ: {thoiGianViet(booking.hetHanGiuChoLuc)}
                      </div>
                    )}

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="rounded-2xl bg-slate-50 p-4 border border-slate-100 space-y-1">
                        <p className="text-xs font-bold uppercase text-slate-400">Thông tin khách hàng</p>
                        <p className="font-bold text-slate-800">{selected.khach.hoTen}</p>
                        <p className="text-xs text-slate-600">{selected.khach.email}</p>
                        <p className="text-xs text-slate-600">{selected.khach.soDienThoai || 'Chưa cập nhật số điện thoại'}</p>
                      </div>
                      <div className="rounded-2xl bg-slate-50 p-4 border border-slate-100 space-y-1">
                        <p className="text-xs font-bold uppercase text-slate-400">Tài chính & Thanh toán</p>
                        <p className="text-sm font-bold text-indigo-600">Tổng tiền: {tienViet(booking.tongTien)}</p>
                        {booking.uuDai?.soTienGiam > 0 && <p className="text-xs text-emerald-700">Tiền sân: {tienViet(booking.uuDai.tamTinh)} · Giảm {booking.uuDai.tyLeGiam}%: {tienViet(booking.uuDai.soTienGiam)}</p>}
                        {booking.chuyenKhoan?.daBaoLuc && money.trangThai === 'CHUA_THANH_TOAN' && <p className="rounded-lg bg-amber-100 p-2 text-xs font-semibold text-amber-900">Khách báo chuyển khoản lúc {thoiGianViet(booking.chuyenKhoan.daBaoLuc)} · Nội dung: {booking.chuyenKhoan.noiDung}. Kiểm tra ngân hàng trước khi xác nhận thu tiền.</p>}
                        {money.thoiDiemThanhToan && <p className="text-xs text-slate-600">Đã thu lúc: {thoiGianViet(money.thoiDiemThanhToan)} ({money.phuongThuc === 'TIEN_MAT' ? 'Tiền mặt' : 'Chuyển khoản'})</p>}
                        {money.maThamChieu && <p className="text-xs font-mono text-slate-600">Mã GD: {money.maThamChieu}</p>}
                        {money.choHoanTien && <p className="text-xs font-bold text-amber-700">Cần hoàn lại: {tienViet(money.soTien)} cho khách</p>}
                      </div>
                    </div>

                    <div>
                      <p className="text-xs font-bold uppercase text-slate-400 mb-2">Danh sách suất sân đã đặt</p>
                      <ul className="space-y-2">{booking.chiTietDatSans.map(item => (
                        <li key={item.id} className="rounded-xl border border-slate-200 p-3 flex justify-between items-center text-sm">
                          <div>
                            <strong className="text-slate-900">{item.lichSan.san.tenSan}</strong>
                            <p className="text-xs text-slate-500 mt-0.5">📅 {item.lichSan.ngayChoi} | ⏰ {item.lichSan.khungGio.gioBatDau} – {item.lichSan.khungGio.gioKetThuc}</p>
                          </div>
                          <span className="font-semibold text-slate-700">{tienViet(item.donGia)}</span>
                        </li>
                      ))}</ul>
                    </div>

                    {/* Danh sách nút hành động Admin */}
                    <div className="pt-2">
                      <p className="text-xs font-bold uppercase text-slate-400 mb-2">Thao tác xử lý đơn</p>
                      <div className="flex flex-wrap gap-2">
                        {selected.hanhDong.map(value => (
                          <button key={value} disabled={busy} onClick={() => openAction(value)}
                            className={'rounded-xl px-4 py-2.5 text-xs font-bold text-shadow transition shadow-sm disabled:opacity-40 ' + (value === 'HUY' ? 'bg-red-600 hover:bg-red-700 text-white' : 'bg-indigo-600 hover:bg-indigo-700 text-white')}>
                            {labels[value]}
                          </button>
                        ))}
                      </div>
                    </div>

                    {/* Form xử lý khi bấm nút hành động */}
                    {action && (
                      <form onSubmit={submit} className="space-y-4 rounded-2xl border border-indigo-200 bg-indigo-50/50 p-5 shadow-inner">
                        <h3 className="font-bold text-indigo-900 text-base">{labels[action]}</h3>
                        
                        {action === 'THANH_TOAN' && (
                          <div className="space-y-3">
                            <p className="text-xs text-slate-600">Ghi nhận thanh toán đủ số tiền: <strong>{tienViet(booking.tongTien)}</strong></p>
                            <label className="block text-xs font-semibold text-slate-700">Phương thức thanh toán
                              <select value={method} onChange={event => setMethod(event.target.value)} className="mt-1 w-full rounded-xl border bg-white p-2.5 text-sm">
                                <option value="TIEN_MAT">Tiền mặt</option>
                                <option value="CHUYEN_KHOAN">Chuyển khoản ngân hàng</option>
                              </select>
                            </label>
                          </div>
                        )}

                        {(action === 'THANH_TOAN' || action === 'HOAN_TIEN') && (
                          <label className="block text-xs font-semibold text-slate-700">
                            {action === 'HOAN_TIEN' ? 'Mã biên nhận / Giao dịch hoàn tiền' : 'Mã giao dịch chuyển khoản (nếu có)'}
                            <input required={action === 'HOAN_TIEN' || method === 'CHUYEN_KHOAN'} maxLength={100} value={reference} onChange={event => setReference(event.target.value)} placeholder="Nhập mã tham chiếu..." className="mt-1 w-full rounded-xl border bg-white p-2.5 text-sm" />
                            {action === 'THANH_TOAN' && method === 'CHUYEN_KHOAN' && <span className="mt-2 block font-normal text-slate-500">Nhập mã giao dịch thực tế từ ngân hàng. Nội dung {booking.chuyenKhoan?.noiDung} dùng để tìm đơn.</span>}
                          </label>
                        )}

                        {(action === 'HUY' || action === 'HOAN_TIEN') && (
                          <label className="block text-xs font-semibold text-slate-700">
                            {action === 'HUY' ? 'Lý do hủy đơn (bắt buộc)' : 'Ghi chú hoàn tiền'}
                            <textarea required maxLength={500} rows={3} value={note} onChange={event => setNote(event.target.value)} placeholder="Nhập lý do chi tiết..." className="mt-1 w-full rounded-xl border bg-white p-2.5 text-sm" />
                          </label>
                        )}

                        {action === 'HUY' && <p className="text-xs text-red-700 font-medium">⚠️ Thao tác này sẽ giải phóng toàn bộ suất sân.{money.trangThai === 'DA_THANH_TOAN' ? ' Đơn sẽ chuyển sang trạng thái chờ hoàn tiền cho khách.' : ''}</p>}
                        {action === 'HOAN_TIEN' && <p className="text-xs text-slate-700 font-medium">Xác nhận đã chuyển khoản/hoàn tiền mặt đủ <strong>{tienViet(money.soTien)}</strong> cho khách hàng.</p>}

                        {['THANH_TOAN', 'HOAN_TIEN'].includes(action) && (
                          <label className="flex items-start gap-2.5 text-xs font-medium text-slate-700 bg-white p-3 rounded-xl border border-indigo-100">
                            <input type="checkbox" required checked={acknowledged} onChange={event => setAcknowledged(event.target.checked)} className="mt-0.5 h-4 w-4 rounded text-indigo-600" />
                            Tôi đã kiểm tra kỹ và xác nhận {action === 'THANH_TOAN' ? 'đã nhận đủ tiền' : 'đã hoàn trả đủ tiền'} khớp với thực tế giao dịch.
                          </label>
                        )}

                        <div className="flex gap-2 pt-2">
                          <button disabled={busy} className="rounded-xl bg-indigo-600 px-5 py-2.5 text-xs font-bold text-white hover:bg-indigo-700 transition shadow">
                            {busy ? 'Đang xử lý...' : 'Xác nhận ngay'}
                          </button>
                          <button type="button" disabled={busy} onClick={() => setAction('')} className="rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition">
                            Hủy bỏ
                          </button>
                        </div>
                      </form>
                    )}

                    {/* Lịch sử xử lý đơn */}
                    <div className="border-t pt-4">
                      <h3 className="text-xs font-bold uppercase text-slate-400 mb-3">Lịch sử thay đổi & Nhật ký xử lý</h3>
                      <ol className="space-y-3 border-l-2 border-indigo-100 pl-4">
                        {selected.lichSu.map(event => (
                          <li key={event.id} className="text-xs space-y-0.5">
                            <p className="font-semibold text-slate-800">{event.ghiChu}</p>
                            <p className="text-slate-400">👤 {event.nguoiThucHien || 'Hệ thống tự động'} · 🕒 {thoiGianViet(event.ngayTao)}</p>
                          </li>
                        ))}
                      </ol>
                    </div>

                  </div>
                )}
              </section>
            </div>
          </div>
  </div>;
}
