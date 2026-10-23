import { useState } from 'react';
import useDuLieu from '../hooks/useDuLieu';
import api, { layThongBaoLoi } from '../services/api';
import { usePhienDangNhap } from '../context/PhienContext';
export default function TaiKhoanPage() {
  const { nguoiDung } = usePhienDangNhap();
  const [search, setSearch] = useState(''), [page, setPage] = useState(0);
  const { data, error, loading, reload } = useDuLieu('/quan-tri/tai-khoan', { tim: search, page });
  const [form, setForm] = useState(null), [busy, setBusy] = useState(false), [saveError, setSaveError] = useState('');
  async function save(e) {
    e.preventDefault(); if (busy) return; setBusy(true); setSaveError('');
    try {
      const payload = { hoTen: form.hoTen.trim(), email: form.email.trim(), soDienThoai: form.soDienThoai.trim(), vaiTro: form.vaiTro, dangHoatDong: form.dangHoatDong };
      if (!form.id) payload.matKhau = form.matKhau;
      await api[form.id ? 'put' : 'post']('/quan-tri/tai-khoan'+(form.id ? '/'+form.id : ''), payload);
      setForm(null); reload();
    } catch (err) { setSaveError(layThongBaoLoi(err)); } finally { setBusy(false); }
  }
  const own = form?.email === nguoiDung?.email;
  return <div className="space-y-5"><div className="flex flex-wrap justify-between gap-3"><h1 className="ql-title">Tài khoản & nhân viên</h1><button className="ql-button-primary" onClick={() => { setForm({ hoTen: '', email: '', soDienThoai: '', matKhau: '', vaiTro: 'STAFF', dangHoatDong: true }); setSaveError(''); }}>Tạo tài khoản</button></div>
    <label className="block max-w-sm">Tìm tên, email, điện thoại<input className="ql-input" maxLength={100} value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} /></label>
    {error && <p role="alert" className="ql-error">{error}</p>}{loading && <p>Đang tải...</p>}
    {data && <><div className="ql-card overflow-auto"><table className="ql-table"><thead><tr><th>Họ tên</th><th>Email</th><th>Điện thoại</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{data.items.map(user => <tr key={user.id}><td>{user.hoTen}</td><td>{user.email}</td><td>{user.soDienThoai || '—'}</td><td>{user.vaiTro}</td><td>{user.dangHoatDong ? 'Hoạt động' : 'Đã khóa'}</td><td><button className="ql-button" onClick={() => { setForm({...user, soDienThoai: user.soDienThoai || '', dangHoatDong: !!user.dangHoatDong }); setSaveError(''); }}>Chỉnh sửa</button></td></tr>)}</tbody></table></div>
    <div className="flex items-center justify-between text-sm"><button disabled={page === 0} className="ql-button" onClick={() => setPage(p => p-1)}>Trước</button><span>{data.totalElements} tài khoản · Trang {page+1}/{Math.max(1,data.totalPages)}</span><button disabled={page+1 >= data.totalPages} className="ql-button" onClick={() => setPage(p => p+1)}>Sau</button></div></>}
    {form && <div className="fixed inset-0 z-50 overflow-auto bg-black/50 p-4"><form onSubmit={save} className="ql-card mx-auto my-8 max-w-lg space-y-4">
      <h2 className="text-xl font-bold">{form.id ? 'Cập nhật tài khoản' : 'Tạo tài khoản'}</h2>{saveError && <p role="alert" className="ql-error">{saveError}</p>}
      <label className="block">Họ tên<input className="ql-input" required maxLength={100} value={form.hoTen} onChange={e => setForm({...form, hoTen:e.target.value})} /></label>
      <label className="block">Email đăng nhập<input className="ql-input" required type="email" readOnly={!!form.id} maxLength={254} value={form.email} onChange={e => setForm({...form, email:e.target.value})} /></label>
      <label className="block">Điện thoại<input className="ql-input" type="tel" value={form.soDienThoai} onChange={e => setForm({...form, soDienThoai:e.target.value})} /></label>
      {!form.id && <label className="block">Mật khẩu ban đầu<input className="ql-input" type="password" autoComplete="new-password" required minLength={8} maxLength={72} value={form.matKhau} onChange={e => setForm({...form, matKhau:e.target.value})} /></label>}
      <label className="block">Vai trò<select className="ql-input" disabled={own} value={form.vaiTro} onChange={e => setForm({...form, vaiTro:e.target.value})}><option value="CUSTOMER">Khách hàng</option><option value="STAFF">Nhân viên</option><option value="ADMIN">Quản trị viên</option></select></label>
      <label className="flex gap-2"><input type="checkbox" disabled={own} checked={form.dangHoatDong} onChange={e => setForm({...form, dangHoatDong:e.target.checked})} />Đang hoạt động</label>
      <p className="text-xs text-slate-500">Khóa tài khoản sẽ ngăn các yêu cầu tiếp theo dù token còn hạn. Không tự đổi quyền/khóa tài khoản đang đăng nhập; hệ thống giữ ít nhất một admin hoạt động.</p>
      <div className="flex gap-2"><button disabled={busy} className="ql-button-primary">{busy ? 'Đang lưu...' : 'Lưu'}</button><button disabled={busy} type="button" className="ql-button" onClick={() => setForm(null)}>Đóng</button></div>
    </form></div>}
  </div>;
}
