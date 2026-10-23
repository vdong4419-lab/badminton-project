import { NavLink, Outlet, Link } from 'react-router-dom';
import { usePhienDangNhap } from '../context/PhienContext';
export default function QuanTriDon() {
  const { nguoiDung, dangXuat } = usePhienDangNhap();
  const admin = nguoiDung?.vaiTro === 'ADMIN';
  const menu = [
    ...(admin ? [['tong-quan','Tổng quan']] : []),
    ['lich-san','Lịch sân & đặt tại quầy'], ['don-dat','Đơn đặt sân'], ['thu-ca','Thu tiền của tôi'],
    ...(admin ? [['san','Sân & giá mặc định'], ['bao-cao','Báo cáo thu / hoàn'], ['tai-khoan','Tài khoản & nhân viên'], ['nhat-ky','Nhật ký quản trị']] : []),
  ];
  return <div className="ql-page min-h-screen bg-slate-100 text-slate-800 lg:flex">
    <aside className="bg-slate-950 text-slate-200 lg:w-64 lg:shrink-0 lg:min-h-screen">
      <div className="border-b border-slate-800 p-5"><p className="text-xs font-bold tracking-widest text-indigo-300">BADMINTON</p><h1 className="text-xl font-bold text-white">Quản lý sân</h1><p className="mt-2 text-xs">{admin ? 'Chủ sân / quản trị viên' : 'Nhân viên vận hành'}</p></div>
      <nav className="flex gap-1 overflow-x-auto p-3 lg:flex-col">{menu.map(([path,title]) =>
        <NavLink key={path} to={'/quan-tri/'+path} className={({ isActive }) => 'whitespace-nowrap rounded-xl px-4 py-3 text-sm '+(isActive ? 'bg-indigo-600 text-white' : 'hover:bg-slate-800')}>{title}</NavLink>)}</nav>
    </aside>
    <div className="min-w-0 flex-1">
      <header className="flex flex-wrap items-center justify-between gap-3 border-b bg-white px-5 py-4"><Link to="/ca-nhan" className="font-semibold">{nguoiDung?.hoTen}</Link><div className="flex items-center gap-4 text-sm"><Link to="/">Trang khách hàng</Link><button onClick={dangXuat} className="ql-button">Đăng xuất</button></div></header>
      <main className="mx-auto max-w-7xl p-4 md:p-6"><Outlet /></main>
    </div>
  </div>;
}
