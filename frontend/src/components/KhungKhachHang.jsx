import { Link, NavLink } from 'react-router-dom';
import { usePhienDangNhap } from '../context/PhienContext';
import LienKetQuanTri from './LienKetQuanTri';
import ChuongThongBao from './ChuongThongBao';

export default function KhungKhachHang({ tieuDe, children }) {
  const { nguoiDung, dangXuat } = usePhienDangNhap();
  const links = [['/', 'Trang chủ'], ['/lich-su', 'Sân đã đặt'], ['/thong-bao', 'Thông báo'], ['/ca-nhan', 'Trang cá nhân']];
  return <div className="min-h-screen bg-slate-50 md:flex">
    <aside className="hidden w-64 shrink-0 flex-col bg-slate-900 text-white md:flex">
      <Link to="/" className="border-b border-slate-800 p-6 text-lg font-bold">🏸 BadmintonHub</Link>
      <nav className="space-y-2 p-4" aria-label="Điều hướng khách hàng">{links.map(([url, label]) =>
        <NavLink key={url} to={url} end className={({ isActive }) => `block rounded-xl px-4 py-3 text-sm font-semibold ${isActive ? 'bg-emerald-600 text-white' : 'text-slate-300 hover:bg-slate-800'}`}>{label}</NavLink>)}</nav>
      <div className="mt-auto p-6 text-xs leading-6 text-slate-400">Chọn lịch phù hợp.<br />Gặp nhau trên sân.</div>
    </aside>
    <div className="min-w-0 flex-1">
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 py-4 sm:px-8">
        <h1 className="text-lg font-bold text-slate-900">{tieuDe}</h1>
        <div className="flex flex-wrap items-center gap-3 text-sm"><ChuongThongBao /><LienKetQuanTri /><Link to="/ca-nhan" className="font-semibold text-emerald-700">{nguoiDung?.hoTen}</Link><button onClick={dangXuat} className="rounded-lg border border-slate-200 px-3 py-2 text-slate-600">Đăng xuất</button></div>
        <nav className="flex w-full flex-wrap gap-4 text-sm md:hidden" aria-label="Điều hướng khách hàng">{links.map(([url, label]) => <NavLink key={url} end to={url} className={({ isActive }) => isActive ? 'font-bold text-emerald-700' : 'text-slate-500'}>{label}</NavLink>)}</nav>
      </header>
      <main className="khach-page mx-auto max-w-6xl space-y-6 p-4 sm:p-8">{children}</main>
    </div>
  </div>;
}
