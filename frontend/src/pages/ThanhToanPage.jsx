import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { layThongBaoLoi } from '../services/api';
import { tienViet, thoiGianViet, tenTrangThaiDon, tenTrangThaiTien } from '../utils/hienThi';
import KhungKhachHang from '../components/KhungKhachHang';

export default function ThanhToanPage() {
  const { donId } = useParams();
  const [don, setDon] = useState(null);
  const [loi, setLoi] = useState('');
  const [thongBao, setThongBao] = useState('');
  const [busy, setBusy] = useState(false);
  const [lanTai, setLanTai] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let running = false;
    async function load() {
      if (running) return;
      running = true;
      try {
        const { data } = await api.get(`/dat-san/${donId}`, { signal: controller.signal });
        if (!controller.signal.aborted) { setDon(data); setLoi(''); }
      } catch (error) { if (!controller.signal.aborted) { setDon(null); setLoi(layThongBaoLoi(error, 'Không tải được đơn đặt sân.')); } }
      finally { running = false; }
    }
    void load();
    const timer = setInterval(load, 10000);
    return () => { controller.abort(); clearInterval(timer); };
  }, [donId, lanTai]);

  async function saoChep(value) {
    try { await navigator.clipboard.writeText(value); setThongBao('Đã sao chép: ' + value); }
    catch { setThongBao('Bạn có thể chọn và sao chép trực tiếp thông tin trên màn hình.'); }
  }
  async function baoDaChuyen() {
    if (busy) return;
    setBusy(true); setLoi('');
    try {
      const { data } = await api.post(`/dat-san/${donId}/bao-chuyen-khoan`);
      setDon(data); setThongBao('Đã gửi thông báo cho sân. Thanh toán sẽ được cập nhật sau khi đối chiếu giao dịch.');
    } catch (error) { setLoi(layThongBaoLoi(error)); setLanTai(v => v + 1); }
    finally { setBusy(false); }
  }
  const ck = don?.chuyenKhoan;
  return <KhungKhachHang tieuDe="Thanh toán đặt sân">
    <Link to="/lich-su" className="text-sm font-semibold text-emerald-700">← Sân đã đặt</Link>
    {loi && <div className="kh-error" role="alert">{loi}<button onClick={() => setLanTai(v => v + 1)} className="ml-3 underline">Tải lại</button></div>}
    {thongBao && <p className="kh-success" role="status">{thongBao}</p>}
    {!don ? !loi && <p className="py-16 text-center text-slate-500">Đang tải thông tin thanh toán...</p> : <>
      <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-widest text-slate-500">Đơn đặt sân #{don.id}</p><h2 className="mt-2 text-2xl font-bold text-slate-900">{tenTrangThaiDon(don.trangThai)}</h2></div><span className="rounded-full bg-emerald-100 px-4 py-2 text-sm font-semibold text-emerald-800">{tenTrangThaiTien(don.thanhToan)}</span></div>
      {don.trangThai === 'DA_HOAN_THANH' && <Link to={`/danh-gia/${don.id}`} className="kh-primary">★ Đánh giá sân và dịch vụ sau buổi chơi</Link>}
      <div className="grid items-start gap-6 lg:grid-cols-2">
        <section className="kh-card space-y-5"><h3 className="kh-heading">Thông tin đơn đặt</h3><ul className="space-y-3">{don.chiTietDatSans.map(item => <li key={item.id} className="rounded-xl bg-slate-50 p-4 text-sm"><p className="font-bold">{item.lichSan.san.tenSan}</p><p className="mt-1 text-slate-500">{item.lichSan.ngayChoi} · {item.lichSan.khungGio.gioBatDau.slice(0,5)} – {item.lichSan.khungGio.gioKetThuc.slice(0,5)}</p><p className="mt-2 font-semibold">{tienViet(item.donGia)}</p></li>)}</ul>
          <dl className="space-y-3 border-t border-slate-200 pt-5 text-sm"><div className="flex justify-between gap-4"><dt>Tiền sân</dt><dd>{tienViet(don.uuDai?.tamTinh ?? don.tongTien)}</dd></div>{don.uuDai?.soTienGiam > 0 && <div className="flex justify-between gap-4 text-emerald-700"><dt>Ưu đãi khách quen ({don.uuDai.tyLeGiam}%)</dt><dd>−{tienViet(don.uuDai.soTienGiam)}</dd></div>}<div className="flex justify-between gap-4 border-t border-slate-200 pt-3 text-lg font-bold"><dt>Tổng thanh toán</dt><dd className="text-emerald-700">{tienViet(don.tongTien)}</dd></div></dl>
          {don.hetHanGiuChoLuc && <p className="rounded-xl bg-amber-50 p-4 text-sm leading-6 text-amber-900">Đang giữ suất đến {thoiGianViet(don.hetHanGiuChoLuc)}. Vui lòng chờ sân xác nhận đơn trước khi chuyển khoản. Trạng thái tự cập nhật mỗi 10 giây.</p>}
          {don.thanhToan?.choHoanTien && <p className="kh-success">Đơn đã hủy, đang chờ admin hoàn đủ {tienViet(don.thanhToan.soTien)} đã thu.</p>}
          {don.thanhToan?.thoiDiemHoanTien && <p className="kh-success">Đã hoàn tiền lúc {thoiGianViet(don.thanhToan.thoiDiemHoanTien)}.</p>}
        </section>
        <section className="kh-card">
          {ck?.coTheChuyenKhoan ? <><h3 className="kh-heading">Chuyển khoản ngân hàng</h3><p className="mt-2 text-sm leading-6 text-slate-500">Quét QR bằng ứng dụng ngân hàng, nhập đúng số tiền và nội dung bên dưới.</p>
            <a href="/images/thanh-toan-mb.jpg" target="_blank" rel="noreferrer" className="mt-5 block"><img src="/images/thanh-toan-mb.jpg" alt="Mã QR chuyển khoản MB của PHAN VAN DONG, số tài khoản 0388465777" className="mx-auto h-auto w-full max-w-sm rounded-2xl border border-slate-200" /></a>
            <dl className="mt-5 space-y-4 text-sm"><div><dt className="text-slate-500">Ngân hàng / người nhận</dt><dd className="mt-1 font-bold">MB · PHAN VAN DONG</dd></div><div><dt className="text-slate-500">Số tài khoản</dt><dd className="mt-1 flex flex-wrap items-center justify-between gap-2"><strong className="select-all text-lg">0388465777</strong><button className="kh-secondary" onClick={() => saoChep('0388465777')}>Sao chép</button></dd></div><div><dt className="text-slate-500">Số tiền cần chuyển</dt><dd className="mt-1 text-2xl font-bold text-emerald-700">{tienViet(don.tongTien)}</dd></div><div className="rounded-xl bg-emerald-50 p-4"><dt className="text-emerald-800">Nội dung chuyển khoản</dt><dd className="mt-2 flex flex-wrap items-center justify-between gap-2"><strong className="select-all text-xl">{ck.noiDung}</strong><button className="kh-secondary" onClick={() => saoChep(ck.noiDung)}>Sao chép</button></dd></div></dl>
            <a href="/images/thanh-toan-mb.jpg" download="QR-MB-PHAN-VAN-DONG.jpg" className="mt-4 inline-block text-sm font-semibold text-emerald-700 underline">Tải ảnh QR</a>
            {ck.daBaoLuc ? <p className="kh-success mt-5" role="status">Bạn đã báo chuyển khoản lúc {thoiGianViet(ck.daBaoLuc)}. Sân đang đối chiếu giao dịch; bạn không cần chuyển lại.</p> : <button className="kh-primary mt-5 w-full" disabled={busy} onClick={baoDaChuyen}>{busy ? 'Đang gửi...' : 'Tôi đã chuyển khoản'}</button>}
            <p className="mt-3 text-xs leading-5 text-slate-500">Trạng thái chỉ đổi sang “Đã thanh toán” khi nhân viên hoặc admin xác nhận nhận tiền. Nút thông báo không tự xác nhận giao dịch.</p>
          </> : <div className="py-10 text-center"><div className="text-4xl" aria-hidden="true">{don.thanhToan?.trangThai === 'DA_THANH_TOAN' ? '✓' : '🏸'}</div><h3 className="mt-4 text-lg font-bold">{don.thanhToan?.trangThai === 'DA_THANH_TOAN' ? 'Sân đã xác nhận nhận tiền' : don.trangThai === 'CHO_XAC_NHAN' ? 'Đang chờ sân xác nhận đơn' : 'Thông tin thanh toán của đơn'}</h3><p className="mt-3 text-sm leading-6 text-slate-500">{don.trangThai === 'CHO_XAC_NHAN' ? 'QR chuyển khoản sẽ hiển thị khi đơn được xác nhận và chưa đến giờ chơi.' : don.trangThai === 'DA_HUY' ? 'Đơn đã hủy. Vui lòng không chuyển tiền cho đơn này.' : don.thanhToan?.trangThai === 'DA_THANH_TOAN' ? `Đã thu ${tienViet(don.thanhToan.soTien)} lúc ${thoiGianViet(don.thanhToan.thoiDiemThanhToan)}.` : 'Đơn này không còn nhận chuyển khoản trực tuyến. Liên hệ nhân viên sân nếu cần hỗ trợ.'}</p></div>}
        </section>
      </div>
    </>}
  </KhungKhachHang>;
}
