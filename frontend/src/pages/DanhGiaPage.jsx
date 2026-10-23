import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';
import KhungKhachHang from '../components/KhungKhachHang';
import { thoiGianViet } from '../utils/hienThi';

function FormDanhGia({ san, donId, onSaved }) {
  const [diem,setDiem] = useState(5), [binhLuan,setBinhLuan] = useState('');
  const [busy,setBusy] = useState(false), [error,setError] = useState('');
  async function gui(event) {
    event.preventDefault(); if (busy) return;
    setBusy(true); setError('');
    try { const { data } = await api.post(`/dat-san/${donId}/danh-gia`, { sanId: san.sanId, diem, binhLuan: binhLuan.trim() }); onSaved(data); }
    catch (err) { setError(layThongBaoLoi(err)); }
    finally { setBusy(false); }
  }
  return <form onSubmit={gui} className="mt-5 space-y-4">
    {error && <p className="kh-error" role="alert">{error}</p>}
    <fieldset><legend className="text-sm font-semibold">Bạn hài lòng với sân ở mức nào?</legend><div className="mt-3 flex flex-wrap gap-3">{[1,2,3,4,5].map(value => <label key={value} className={`cursor-pointer rounded-xl border px-3 py-2 text-sm ${diem === value ? 'border-amber-500 bg-amber-50 text-amber-900' : 'border-slate-200'}`}><input type="radio" name={`diem-${san.sanId}`} value={value} checked={diem === value} onChange={() => setDiem(value)} className="mr-2" />{value} ★</label>)}</div></fieldset>
    <label className="kh-label">Chia sẻ về sân và dịch vụ<textarea className="kh-input" required minLength={10} maxLength={2000} rows={5} value={binhLuan} onChange={e => setBinhLuan(e.target.value)} placeholder="Chất lượng mặt sân, ánh sáng, vệ sinh và thái độ phục vụ..." /></label>
    <p className="text-xs text-slate-500">10–2000 ký tự. Đánh giá sẽ hiển thị công khai cùng họ tên của bạn.</p>
    <button className="kh-primary" disabled={busy}>{busy ? 'Đang gửi...' : 'Gửi đánh giá'}</button>
  </form>;
}
export default function DanhGiaPage() {
  const { donId } = useParams();
  const [data,setData] = useState(null), [error,setError] = useState(''), [lanTai,setLanTai] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      try { const result = await api.get(`/dat-san/${donId}/danh-gia`, { signal: controller.signal }); if (!controller.signal.aborted) { setData(result.data); setError(''); } }
      catch (err) { if (!controller.signal.aborted) setError(layThongBaoLoi(err)); }
    }
    void load(); return () => controller.abort();
  }, [donId,lanTai]);
  return <KhungKhachHang tieuDe={`Đánh giá trải nghiệm · Đơn #${donId}`}>
    <Link to="/lich-su" className="text-sm font-semibold text-emerald-700">← Sân đã đặt</Link>
    <p className="text-sm leading-6 text-slate-500">Sau khi chơi xong, thanh toán và được sân hoàn thành đơn, bạn có thể đánh giá từng sân trong đơn một lần.</p>
    {error && <p className="kh-error" role="alert">{error}<button className="ml-3 underline" onClick={() => setLanTai(v => v + 1)}>Thử lại</button></p>}
    {!data ? !error && <p>Đang tải...</p> : data.map(san => <section key={san.sanId} className="kh-card"><Link to={`/san/${san.sanId}`} className="kh-heading hover:text-emerald-700">{san.tenSan} →</Link>
      {san.daDanhGia ? <div className="mt-4 rounded-xl bg-emerald-50 p-5"><p className="font-bold text-amber-700">{'★'.repeat(san.daDanhGia.diem)} · {san.daDanhGia.diem}/5</p><p className="mt-3 whitespace-pre-wrap break-words text-sm">{san.daDanhGia.binhLuan}</p><p className="mt-3 text-xs text-slate-500">Đã đánh giá lúc {thoiGianViet(san.daDanhGia.ngayTao)}</p></div>
        : san.coTheDanhGia ? <FormDanhGia san={san} donId={donId} onSaved={setData} /> : <p className="mt-4 text-sm text-slate-500">Đơn này chưa đủ điều kiện đánh giá. Vui lòng chờ hoàn thành đơn sau buổi chơi.</p>}
    </section>)}
  </KhungKhachHang>;
}
