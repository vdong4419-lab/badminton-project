import { useEffect, useId, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import useThongBao from '../hooks/useThongBao';
import { thoiGianViet } from '../utils/hienThi';

export default function ChuongThongBao() {
  const { data, error, busy, danhDau, taiLai, email } = useThongBao();
  const [open, setOpen] = useState(false);
  const ref = useRef(null), id = useId(), navigate = useNavigate();
  useEffect(() => {
    function close(event) { if (!ref.current?.contains(event.target)) setOpen(false); }
    function escape(event) { if (event.key === 'Escape') setOpen(false); }
    document.addEventListener('mousedown', close); document.addEventListener('keydown', escape);
    return () => { document.removeEventListener('mousedown', close); document.removeEventListener('keydown', escape); };
  }, []);
  if (!email) return null;
  async function moDon(item) {
    if (await danhDau(item.id)) { setOpen(false); navigate(`/thanh-toan/${item.donId}`); }
  }
  return <div ref={ref} className="relative">
    <button type="button" onClick={() => { setOpen(v => !v); if (!open) taiLai(); }} aria-expanded={open} aria-controls={id} aria-label={`Thông báo, ${data?.totalUnread || 0} chưa đọc`} className="relative rounded-xl border border-slate-200 bg-white p-2.5 text-slate-700 hover:bg-slate-50">
      <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9Z"/><path d="M10 21h4"/></svg>
      {data?.totalUnread > 0 && <span className="absolute -right-2 -top-2 rounded-full bg-red-600 px-1.5 py-0.5 text-[10px] font-bold text-white">{data.totalUnread > 99 ? '99+' : data.totalUnread}</span>}
    </button>
    {open && <section id={id} aria-label="Thông báo đặt sân" className="fixed left-4 right-4 top-24 z-50 overflow-hidden rounded-2xl border border-slate-200 bg-white text-slate-800 shadow-xl sm:absolute sm:left-auto sm:right-0 sm:top-full sm:mt-3 sm:w-[22rem]">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b p-4"><strong>Thông báo</strong><button disabled={busy || !data?.totalUnread} className="text-xs font-semibold text-emerald-700 disabled:opacity-50" onClick={() => danhDau()}>Đọc tất cả</button></div>
      {error && <p role="alert" className="bg-red-50 p-3 text-xs text-red-700">{error}<button onClick={taiLai} className="ml-2 underline">Thử lại</button></p>}
      <ul className="max-h-96 overflow-y-auto">{!data ? <li className="p-5 text-sm text-slate-500">Đang tải thông báo...</li> : !data.items.length ? <li className="p-5 text-sm text-slate-500">Bạn chưa có thông báo đặt sân.</li> : data.items.map(item => <li key={item.id}><button disabled={busy} onClick={() => moDon(item)} className={`block w-full border-b border-slate-100 p-4 text-left hover:bg-slate-50 ${item.daDoc ? '' : 'bg-emerald-50'}`}><p className="text-sm font-semibold">{!item.daDoc && <span className="mr-1 text-emerald-600">●</span>}{item.tieuDe}</p><p className="mt-1 text-xs leading-5 text-slate-600">{item.noiDung}</p><time className="mt-2 block text-xs text-slate-400">{thoiGianViet(item.ngayTao)}</time></button></li>)}</ul>
      <Link to="/thong-bao" onClick={() => setOpen(false)} className="block p-3 text-center text-sm font-semibold text-emerald-700">Xem tất cả thông báo →</Link>
    </section>}
  </div>;
}
