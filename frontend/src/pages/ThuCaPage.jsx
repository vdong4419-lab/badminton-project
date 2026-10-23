import useDuLieu from '../hooks/useDuLieu';
import { tienViet, thoiGianViet } from '../utils/hienThi';
export default function ThuCaPage() {
  const { data, error, loading, reload } = useDuLieu('/quan-tri/thu-ca');
  return <div className="space-y-5"><h1 className="ql-title">Tiền tôi ghi nhận hôm nay</h1><button onClick={reload} className="ql-button">Tải lại</button>{error && <p className="ql-error">{error}</p>}{loading && <p>Đang tải...</p>}
    {data && <><section className="ql-card"><p>{data.ngay} · {data.giaoDich.length} giao dịch</p><strong className="mt-3 block text-3xl">{tienViet(data.tongThu)}</strong><p className="mt-2 text-sm text-slate-500">Tổng tiền do chính tài khoản này xác nhận thu trong ngày; không phải báo cáo thu ròng sau hoàn tiền.</p></section><div className="ql-card overflow-auto"><table className="ql-table"><thead><tr><th>Đơn</th><th>Thời điểm</th><th>Phương thức</th><th>Số tiền</th><th>Mã giao dịch</th></tr></thead><tbody>{data.giaoDich.map(t => <tr key={t.id}><td>#{t.donId}</td><td>{thoiGianViet(t.thoiDiemThanhToan)}</td><td>{t.phuongThuc === 'TIEN_MAT' ? 'Tiền mặt' : 'Chuyển khoản'}</td><td>{tienViet(t.soTien)}</td><td>{t.maThamChieu || '—'}</td></tr>)}</tbody></table></div></>}
  </div>;
}
