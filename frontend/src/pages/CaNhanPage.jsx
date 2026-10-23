import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';
import { usePhienDangNhap } from '../context/PhienContext';
import { tienViet, thoiGianViet } from '../utils/hienThi';
import KhungKhachHang from '../components/KhungKhachHang';

export default function CaNhanPage() {
  const { capNhatHoSo, dangXuat } = usePhienDangNhap();
  const navigate = useNavigate();
  const [hoSo, setHoSo] = useState(null);
  const [form, setForm] = useState({ hoTen: '', soDienThoai: '' });
  const [password, setPassword] = useState({ matKhauHienTai: '', matKhauMoi: '', xacNhan: '' });
  const [loi, setLoi] = useState('');
  const [thongBao, setThongBao] = useState('');
  const [loiMatKhau, setLoiMatKhau] = useState('');
  const [busy, setBusy] = useState('');
  const [lanTai, setLanTai] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      setLoi('');
      try {
        const { data } = await api.get('/ho-so', { signal: controller.signal });
        if (!controller.signal.aborted) { setHoSo(data); setForm({ hoTen: data.hoTen, soDienThoai: data.soDienThoai || '' }); }
      } catch (error) { if (!controller.signal.aborted) setLoi(layThongBaoLoi(error, 'Không tải được trang cá nhân.')); }
    }
    void load();
    return () => controller.abort();
  }, [lanTai]);

  async function luuHoSo(event) {
    event.preventDefault();
    if (busy) return;
    setBusy('ho-so'); setLoi(''); setThongBao('');
    try {
      const { data } = await api.put('/ho-so', { hoTen: form.hoTen.trim(), soDienThoai: form.soDienThoai.trim() });
      setHoSo(data); capNhatHoSo(data); setThongBao('Đã cập nhật thông tin cá nhân.');
    } catch (error) { setLoi(layThongBaoLoi(error)); }
    finally { setBusy(''); }
  }

  async function doiMatKhau(event) {
    event.preventDefault();
    if (busy) return;
    setLoiMatKhau('');
    if (password.matKhauMoi !== password.xacNhan) { setLoiMatKhau('Mật khẩu xác nhận chưa trùng khớp.'); return; }
    setBusy('mat-khau');
    try {
      await api.put('/ho-so/mat-khau', { matKhauHienTai: password.matKhauHienTai, matKhauMoi: password.matKhauMoi });
      dangXuat(); navigate('/dang-nhap', { replace: true, state: { thongBao: 'Đã đổi mật khẩu. Vui lòng đăng nhập bằng mật khẩu mới.' } });
    } catch (error) { setLoiMatKhau(layThongBaoLoi(error)); }
    finally { setBusy(''); }
  }

  const member = hoSo?.thanhVien;
  const progress = member ? member.mocTiepTheo ? Math.min(100, member.soDonHoanThanh / member.mocTiepTheo * 100) : 100 : 0;
  return <KhungKhachHang tieuDe="Trang cá nhân">
    {loi && <div className="kh-error" role="alert">{loi} {!hoSo && <button className="ml-3 underline" onClick={() => setLanTai(v => v + 1)}>Thử lại</button>}</div>}
    {thongBao && <p className="kh-success" role="status">{thongBao}</p>}
    {!hoSo ? !loi && <p className="py-16 text-center text-slate-500">Đang tải thông tin của bạn...</p> : <>
      <section className="flex flex-wrap items-center justify-between gap-5 rounded-3xl bg-emerald-950 p-6 text-white sm:p-8">
        <div className="flex min-w-0 items-center gap-4"><div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-emerald-400 text-2xl font-black text-emerald-950">{hoSo.hoTen.trim().charAt(0).toUpperCase()}</div><div className="min-w-0"><p className="text-xs font-semibold uppercase tracking-widest text-emerald-300">Tài khoản của bạn</p><h2 className="mt-1 break-words text-2xl font-bold">{hoSo.hoTen}</h2><p className="mt-1 break-all text-sm text-emerald-100">{hoSo.email}</p></div></div>
        <Link className="rounded-xl bg-white px-5 py-3 text-sm font-bold text-emerald-900" to="/lich-su">Xem sân đã đặt →</Link>
      </section>
      <div className="grid gap-4 sm:grid-cols-3">{[['Đơn hoàn thành và đã trả tiền', member.soDonHoanThanh], ['Chi tiêu cho đơn hoàn thành', tienViet(member.tongChiTieu)], ['Ưu đãi hiện tại', `${member.tyLeGiam}%`]].map(([label, value]) => <div className="kh-card" key={label}><p className="text-xs text-slate-500">{label}</p><p className="mt-2 text-2xl font-bold text-slate-900">{value}</p></div>)}</div>
      <div className="grid items-start gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <section className="kh-card"><h2 className="kh-heading">Thông tin cá nhân</h2><p className="mt-1 text-sm text-slate-500">Cập nhật thông tin để sân liên hệ khi cần.</p>
            <form onSubmit={luuHoSo} className="mt-5 space-y-4">
              <label className="kh-label">Họ và tên<input className="kh-input" autoComplete="name" required maxLength={100} value={form.hoTen} onChange={e => setForm({ ...form, hoTen: e.target.value })} /></label>
              <label className="kh-label">Số điện thoại<input className="kh-input" type="tel" autoComplete="tel" maxLength={16} pattern="\+?[0-9]{9,15}" value={form.soDienThoai} onChange={e => setForm({ ...form, soDienThoai: e.target.value })} placeholder="Chưa cập nhật" /></label>
              <label className="kh-label">Email đăng nhập<input className="kh-input bg-slate-50" type="email" value={hoSo.email} readOnly /></label>
              <p className="text-xs text-slate-500">Ngày tham gia: {thoiGianViet(hoSo.ngayThamGia)}. Email được giữ cố định cho tài khoản.</p>
              <button className="kh-primary" disabled={!!busy}>{busy === 'ho-so' ? 'Đang lưu...' : 'Lưu thông tin'}</button>
            </form>
          </section>
          <section className="kh-card"><h2 className="kh-heading">Đổi mật khẩu</h2><p className="mt-1 text-sm text-slate-500">Bạn sẽ đăng nhập lại sau khi đổi mật khẩu.</p>
            <form onSubmit={doiMatKhau} className="mt-5 space-y-4">
              {loiMatKhau && <p className="kh-error" role="alert">{loiMatKhau}</p>}
              {[['matKhauHienTai', 'Mật khẩu hiện tại', 'current-password'], ['matKhauMoi', 'Mật khẩu mới (từ 8 ký tự)', 'new-password'], ['xacNhan', 'Nhập lại mật khẩu mới', 'new-password']].map(([key, label, complete]) => <label className="kh-label" key={key}>{label}<input className="kh-input" type="password" autoComplete={complete} required minLength={key === 'matKhauHienTai' ? 1 : 8} maxLength={72} value={password[key]} onChange={e => setPassword({ ...password, [key]: e.target.value })} /></label>)}
              <button className="kh-secondary" disabled={!!busy}>{busy === 'mat-khau' ? 'Đang đổi...' : 'Đổi mật khẩu'}</button>
            </form>
          </section>
        </div>
        <section className="kh-card border-emerald-200 bg-emerald-50/50"><p className="text-xs font-bold uppercase tracking-widest text-emerald-700">Quyền lợi thành viên</p><h2 className="mt-2 text-2xl font-bold text-emerald-950">{member.hang}</h2>
          <p className="mt-3 text-sm leading-6 text-slate-600">{hoSo.vaiTro === 'CUSTOMER' ? member.tyLeGiam ? `Giảm ${member.tyLeGiam}% cho mỗi đơn mới, tối đa ${tienViet(member.giamToiDa)}.` : 'Chơi thường xuyên hơn để nhận ưu đãi cho lần đặt tiếp theo.' : 'Ưu đãi khách quen dành cho tài khoản khách hàng.'}</p>
          {hoSo.vaiTro === 'CUSTOMER' && <><div className="mt-5 h-2 overflow-hidden rounded-full bg-emerald-100" role="progressbar" aria-label="Tiến độ hạng thành viên" aria-valuenow={member.soDonHoanThanh} aria-valuemin={0} aria-valuemax={Math.max(member.soDonHoanThanh, member.mocTiepTheo, 1)}><div className="h-full rounded-full bg-emerald-600" style={{ width: `${progress}%` }} /></div><p className="mt-2 text-xs text-emerald-800">{member.mocTiepTheo ? `Thêm ${member.donConLai} đơn hoàn thành để đạt mức giảm ${member.mocTiepTheo === 5 ? 5 : 10}%.` : 'Bạn đã đạt mức ưu đãi cao nhất.'}</p></>}
          <ul className="mt-6 space-y-3 border-t border-emerald-200 pt-5 text-sm text-slate-700"><li>5 đơn: giảm 5%</li><li>10 đơn: giảm 10%</li><li>Tối đa 50.000đ mỗi đơn</li></ul>
          <p className="mt-4 text-xs leading-5 text-slate-500">Chỉ tính đơn đã thanh toán và hoàn thành. Đơn hủy, chờ xử lý hoặc đã hoàn tiền không được tính. Ưu đãi tự động áp dụng khi tạo đơn, kể cả đặt tại quầy.</p>
        </section>
      </div>
    </>}
  </KhungKhachHang>;
}
