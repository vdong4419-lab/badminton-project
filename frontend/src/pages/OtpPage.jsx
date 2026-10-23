import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';

export default function OtpPage({ datLai = false }) {
  const location = useLocation(), navigate = useNavigate();
  const [email, setEmail] = useState(location.state?.email || '');
  const [otp, setOtp] = useState('');
  const [matKhau, setMatKhau] = useState('');
  const [xacNhan, setXacNhan] = useState('');
  const [loi, setLoi] = useState('');
  const [message, setMessage] = useState(location.state?.thongBao || '');
  const [busy, setBusy] = useState('');
  const [remaining, setRemaining] = useState(location.state?.daGui ? 60 : 0);
  useEffect(() => {
    const timer = setInterval(() => setRemaining(v => Math.max(0, v - 1)), 1000);
    return () => clearInterval(timer);
  }, []);
  async function guiMa() {
    if (busy || remaining > 0) return;
    if (!email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) { setLoi('Vui lòng nhập email hợp lệ.'); return; }
    setBusy('gui'); setLoi(''); setMessage('');
    try {
      const { data } = await api.post(datLai ? '/xac-thuc/quen-mat-khau' : '/xac-thuc/gui-lai-otp', { email: email.trim().toLowerCase() });
      setMessage(data.message); setRemaining(60); setOtp('');
    } catch (error) { setLoi(layThongBaoLoi(error)); }
    finally { setBusy(''); }
  }
  async function xacMinh(event) {
    event.preventDefault(); if (busy) return;
    setLoi('');
    if (datLai && matKhau !== xacNhan) { setLoi('Mật khẩu xác nhận chưa khớp.'); return; }
    setBusy('xac-nhan');
    try {
      const { data } = await api.post(datLai ? '/xac-thuc/dat-lai-mat-khau' : '/xac-thuc/xac-minh-email', {
        email: email.trim().toLowerCase(), otp, ...(datLai ? { matKhauMoi: matKhau } : {}),
      });
      navigate('/dang-nhap', { replace: true, state: { thongBao: data.message } });
    } catch (error) { setLoi(layThongBaoLoi(error)); }
    finally { setBusy(''); }
  }
  return <main className="flex min-h-screen items-center justify-center bg-slate-950 p-4">
    <section className="w-full max-w-lg rounded-3xl border border-slate-700 bg-slate-900 p-6 text-white sm:p-9">
      <Link to="/" className="font-bold text-emerald-400">🏸 BadmintonHub</Link>
      <h1 className="mt-7 text-2xl font-bold">{datLai ? 'Quên mật khẩu' : 'Xác minh email'}</h1>
      <p className="mt-3 text-sm leading-6 text-slate-300">{datLai ? 'Nhận OTP qua email để đặt mật khẩu mới.' : 'Nhập mã OTP đã gửi đến email đăng ký để kích hoạt tài khoản.'} Mã có hiệu lực 10 phút, tối đa 5 lần nhập sai.</p>
      {loi && <p role="alert" className="mt-5 rounded-xl bg-red-950 p-4 text-sm text-red-200">{loi}</p>}
      {message && <p role="status" className="mt-5 rounded-xl bg-emerald-950 p-4 text-sm text-emerald-200">{message}</p>}
      <form onSubmit={xacMinh} className="mt-6 space-y-5">
        <label className="block text-sm">Email đăng ký<input className="mt-2 block w-full rounded-xl border border-slate-600 bg-slate-800 p-3" type="email" autoComplete="email" required maxLength={254} value={email} onChange={e => { setEmail(e.target.value); setOtp(''); }} /></label>
        <button type="button" disabled={!!busy || remaining > 0} className="rounded-xl border border-emerald-500 px-4 py-2 text-sm text-emerald-300 disabled:opacity-50" onClick={guiMa}>{busy === 'gui' ? 'Đang gửi...' : remaining > 0 ? `Gửi lại sau ${remaining}s` : 'Gửi mã OTP'}</button>
        <label className="block text-sm">Mã OTP 6 số<input className="mt-2 block w-full rounded-xl border border-slate-600 bg-slate-800 p-3 text-center text-2xl tracking-[.4em]" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" required maxLength={6} value={otp} onChange={e => setOtp(e.target.value.replace(/\D/g, '').slice(0,6))} /></label>
        {datLai && <>
          <label className="block text-sm">Mật khẩu mới<input className="mt-2 block w-full rounded-xl border border-slate-600 bg-slate-800 p-3" type="password" autoComplete="new-password" minLength={8} maxLength={72} required value={matKhau} onChange={e => setMatKhau(e.target.value)} /></label>
          <label className="block text-sm">Nhập lại mật khẩu mới<input className="mt-2 block w-full rounded-xl border border-slate-600 bg-slate-800 p-3" type="password" autoComplete="new-password" minLength={8} maxLength={72} required value={xacNhan} onChange={e => setXacNhan(e.target.value)} /></label>
        </>}
        <button disabled={!!busy} className="w-full rounded-xl bg-emerald-500 p-3 font-bold text-slate-950 disabled:opacity-50">{busy === 'xac-nhan' ? 'Đang xử lý...' : datLai ? 'Đặt lại mật khẩu' : 'Xác minh tài khoản'}</button>
      </form>
      <p className="mt-5 text-xs leading-5 text-slate-400">Kiểm tra cả mục thư rác. Chỉ dùng mã mới nhất trong email tương ứng với thao tác bạn đang thực hiện.</p>
      <Link to="/dang-nhap" className="mt-6 inline-block text-sm font-semibold text-emerald-400">← Quay lại đăng nhập</Link>
    </section>
  </main>;
}
