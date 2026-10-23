import { useState } from 'react';
import useDuLieu from '../hooks/useDuLieu';
import api, { layThongBaoLoi } from '../services/api';
import { tienViet } from '../utils/hienThi';
const blank = { tenSan: '', giaMoiGio: 100000, moTa: '', anhDaiDien: '', dangHoatDong: true };
export default function SanPage() {
  const { data, error, loading, reload } = useDuLieu('/quan-tri/san');
  const [form, setForm] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [saveError, setSaveError] = useState('');
  async function save(e) {
    e.preventDefault(); if (busy) return;
    setBusy(true); setSaveError('');
    try {
      await api[form.id ? 'put' : 'post']('/quan-tri/san'+(form.id ? '/'+form.id : ''), form);
      setForm(null); setMessage('Đã lưu sân. Giá mặc định dùng cho các suất mới; các suất đã tạo giữ giá riêng.'); reload();
    } catch (err) { setSaveError(layThongBaoLoi(err)); } finally { setBusy(false); }
  }
  return <div className="space-y-5"><div className="flex items-center justify-between gap-3"><h1 className="ql-title">Sân & giá mặc định</h1><button className="ql-button" onClick={() => { setForm({...blank}); setSaveError(''); }}>Thêm sân</button></div>
    {error && <p className="ql-error">{error}</p>}{message && <p className="ql-success">{message}</p>}{loading && <p>Đang tải...</p>}
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{(data || []).map(court => <article key={court.id} className="ql-card space-y-3">
      {court.anhDaiDien && <img src={court.anhDaiDien} alt={court.tenSan} className="h-36 w-full rounded-xl object-cover" />}
      <div className="flex justify-between gap-2"><h2 className="font-bold">{court.tenSan}</h2><span className={court.dangHoatDong ? 'text-emerald-700' : 'text-red-700'}>{court.dangHoatDong ? 'Hoạt động' : 'Tạm ngừng'}</span></div>
      <p className="text-sm text-slate-500">{court.moTa || 'Chưa có mô tả'}</p><p className="font-semibold">{tienViet(court.giaMoiGio)}/giờ</p><button className="ql-button" onClick={() => { setForm({...court, moTa: court.moTa || '', anhDaiDien: court.anhDaiDien || ''}); setSaveError(''); }}>Chỉnh sửa</button>
    </article>)}</div>
    {form && <div className="fixed inset-0 z-50 overflow-y-auto bg-black/50 p-4"><form onSubmit={save} className="ql-card mx-auto my-8 max-w-lg space-y-4" aria-label="Thông tin sân">
      <h2 className="text-xl font-bold">{form.id ? 'Chỉnh sửa sân' : 'Thêm sân'}</h2>{saveError && <p role="alert" className="ql-error">{saveError}</p>}
      <label className="block">Tên sân<input className="ql-input" required maxLength={100} value={form.tenSan} onChange={e => setForm({...form, tenSan: e.target.value})} /></label>
      <label className="block">Giá mặc định mỗi giờ<input className="ql-input" type="number" min="0.01" step="0.01" required value={form.giaMoiGio} onChange={e => setForm({...form, giaMoiGio: Number(e.target.value)})} /></label>
      <label className="block">Mô tả<textarea className="ql-input" maxLength={1000} value={form.moTa} onChange={e => setForm({...form, moTa: e.target.value})} /></label>
      <label className="block">URL ảnh (http/https)<input className="ql-input" type="url" maxLength={500} value={form.anhDaiDien} onChange={e => setForm({...form, anhDaiDien: e.target.value})} /></label>
      <label className="flex gap-2"><input type="checkbox" checked={!!form.dangHoatDong} onChange={e => setForm({...form, dangHoatDong: e.target.checked})} />Đang hoạt động</label>
      <p className="text-xs text-slate-500">Sân còn đơn chưa kết thúc cần được xử lý trước khi tạm ngừng. Đổi giá từng suất tại mục Lịch sân.</p>
      <div className="flex gap-2"><button disabled={busy} className="ql-button-primary">{busy ? 'Đang lưu...' : 'Lưu sân'}</button><button type="button" disabled={busy} className="ql-button" onClick={() => setForm(null)}>Đóng</button></div>
    </form></div>}
  </div>;
}
