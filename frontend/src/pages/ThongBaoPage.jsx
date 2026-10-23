import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useThongBao from '../hooks/useThongBao';
import KhungKhachHang from '../components/KhungKhachHang';
import { thoiGianViet } from '../utils/hienThi';
export default function ThongBaoPage() {
  const [page,setPage] = useState(0);
  const { data,error,busy,danhDau,taiLai } = useThongBao(page);
  const navigate = useNavigate();
  async function moDon(item) { if (await danhDau(item.id)) navigate(`/thanh-toan/${item.donId}`); }
  return <KhungKhachHang tieuDe="Thông báo của tôi">
    <div className="flex flex-wrap items-center justify-between gap-3"><p className="text-sm text-slate-500">{data?.totalUnread || 0} thông báo chưa đọc · Cập nhật mỗi 30 giây</p><button className="kh-secondary" disabled={busy || !data?.totalUnread} onClick={() => danhDau()}>Đánh dấu tất cả đã đọc</button></div>
    {error && <p className="kh-error" role="alert">{error}<button className="ml-3 underline" onClick={taiLai}>Thử lại</button></p>}
    <section className="kh-card">{!data ? <p>Đang tải...</p> : !data.items.length ? <p>Bạn chưa có thông báo.</p> : <ul className="divide-y divide-slate-100">{data.items.map(item => <li key={item.id}><button disabled={busy} onClick={() => moDon(item)} className={`w-full rounded-xl p-4 text-left ${item.daDoc ? '' : 'bg-emerald-50'}`}><strong className="text-sm">{!item.daDoc && '● '}{item.tieuDe}</strong><p className="mt-1 text-sm text-slate-600">{item.noiDung}</p><p className="mt-2 text-xs text-slate-400">{thoiGianViet(item.ngayTao)}</p></button></li>)}</ul>}</section>
    <div className="flex justify-between"><button className="kh-secondary" disabled={page === 0} onClick={() => setPage(v => v - 1)}>Trang trước</button><button className="kh-secondary" disabled={!data || page + 1 >= data.totalPages} onClick={() => setPage(v => v + 1)}>Trang sau</button></div>
  </KhungKhachHang>;
}
