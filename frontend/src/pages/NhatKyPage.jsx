import { useState } from 'react';
import useDuLieu from '../hooks/useDuLieu';
import { thoiGianViet } from '../utils/hienThi';
export default function NhatKyPage() {
  const [page, setPage] = useState(0);
  const { data, error, loading } = useDuLieu('/quan-tri/nhat-ky', { page });
  return <div className="space-y-5"><h1 className="ql-title">Nhật ký quản trị</h1><p className="text-sm text-slate-600">Thay đổi sân, lịch và tài khoản. Lịch sử thu/hoàn tiền nằm trong chi tiết từng đơn.</p>{error && <p className="ql-error">{error}</p>}{loading && <p>Đang tải...</p>}
    <div className="ql-card overflow-auto"><table className="ql-table"><thead><tr><th>Thời điểm</th><th>Người thao tác</th><th>Hành động</th><th>Đối tượng</th><th>Nội dung</th></tr></thead><tbody>{(data || []).map(event => <tr key={event.id}><td>{thoiGianViet(event.ngayTao)}</td><td>{event.nguoiThucHien}</td><td>{event.hanhDong}</td><td>{event.doiTuong}</td><td>{event.noiDung}</td></tr>)}</tbody></table></div>
    <div className="flex justify-between"><button className="ql-button" disabled={page === 0} onClick={() => setPage(p => p-1)}>Trước</button><span>Trang {page+1}</span><button className="ql-button" disabled={!data || data.length<20} onClick={() => setPage(p => p+1)}>Sau</button></div>
  </div>;
}
