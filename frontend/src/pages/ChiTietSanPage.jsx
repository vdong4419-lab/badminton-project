import { useEffect, useState } from 'react';
import { Link, useParams, useLocation } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';
import { tienViet, thoiGianViet } from '../utils/hienThi';
import ChuongThongBao from '../components/ChuongThongBao';
import DiemDanhGia from '../components/DiemDanhGia';
export default function ChiTietSanPage() {
  const { sanId } = useParams();
  const { hash } = useLocation();
  const [san,setSan] = useState(null), [reviews,setReviews] = useState(null), [error,setError] = useState('');
  const [page,setPage] = useState(0), [reload,setReload] = useState(0);
  useEffect(() => {
    if (reviews && hash === '#danh-gia') document.getElementById('danh-gia')?.scrollIntoView({ block: 'start' });
  }, [reviews,hash]);
  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      try {
        const [court, ratings] = await Promise.all([api.get(`/san/${sanId}`, { signal: controller.signal }), api.get(`/san/${sanId}/danh-gia`, { params: { page }, signal: controller.signal })]);
        if (!controller.signal.aborted) { setSan(court.data); setReviews(ratings.data); setError(''); }
      } catch (err) { if (!controller.signal.aborted) setError(layThongBaoLoi(err, 'Không tải được thông tin sân.')); }
    }
    void load(); return () => controller.abort();
  }, [sanId,page,reload]);
  return <div className="min-h-screen bg-slate-50"><header className="flex flex-wrap items-center justify-between gap-3 border-b bg-white px-6 py-4"><Link to="/" className="font-bold text-emerald-800">🏸 BadmintonHub</Link><div className="flex items-center gap-4"><ChuongThongBao /><Link to="/" className="text-sm">Trang chủ</Link><Link to="/ca-nhan" className="text-sm">Tài khoản</Link></div></header>
    <main className="khach-page mx-auto max-w-5xl space-y-6 p-4 sm:p-8">
      {error && <p className="kh-error" role="alert">{error}<button className="ml-3 underline" onClick={() => setReload(v => v + 1)}>Thử lại</button></p>}
      {!san || !reviews ? !error && <p>Đang tải thông tin sân...</p> : <>
        <section className="kh-card grid items-center gap-6 md:grid-cols-2"><div>{san.anhDaiDien ? <img src={san.anhDaiDien} alt={san.tenSan} className="aspect-video w-full rounded-2xl object-cover" /> : <div className="flex aspect-video items-center justify-center rounded-2xl bg-emerald-100 text-6xl" aria-hidden="true">🏸</div>}</div><div><h1 className="text-3xl font-bold">{san.tenSan}</h1><p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-500">{san.moTa || 'Sân cầu lông phục vụ đặt lịch trực tuyến.'}</p><div className="mt-3"><DiemDanhGia diem={reviews.diemTrungBinh} soLuong={reviews.tongDanhGia} /></div><p className="mt-3 text-sm text-slate-500">Giá mặc định: {tienViet(san.giaMoiGio)}/giờ. Giá đặt cụ thể được hiển thị theo từng suất.</p><Link to={`/dat-lich/${san.id}`} className="kh-primary mt-5">Chọn lịch đặt sân</Link></div></section>
        <section id="danh-gia" className="kh-card scroll-mt-24"><h2 className="kh-heading">Khách hàng đánh giá</h2><p className="mt-1 text-sm text-slate-500">Chia sẻ từ khách đã thanh toán và hoàn thành buổi chơi.</p>{!reviews.items.length ? <p className="py-8 text-sm text-slate-500">Sân chưa có đánh giá.</p> : <ul className="mt-5 divide-y divide-slate-100">{reviews.items.map(item => <li key={item.id} className="py-5"><div className="flex flex-wrap items-center justify-between gap-2"><strong className="text-sm">{item.khachHang}</strong><span className="font-semibold text-amber-700">{'★'.repeat(item.diem)} {item.diem}/5</span></div><p className="mt-3 whitespace-pre-wrap break-words text-sm leading-6 text-slate-600">{item.binhLuan}</p><p className="mt-3 text-xs text-slate-400">{thoiGianViet(item.ngayTao)}</p></li>)}</ul>}<div className="mt-4 flex justify-between"><button className="kh-secondary" disabled={page === 0} onClick={() => setPage(v => v - 1)}>Trang trước</button><button className="kh-secondary" disabled={page + 1 >= reviews.totalPages} onClick={() => setPage(v => v + 1)}>Trang sau</button></div></section>
      </>}
    </main>
  </div>;
}
