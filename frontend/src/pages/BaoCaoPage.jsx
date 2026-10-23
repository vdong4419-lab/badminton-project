import { useState } from 'react';
import api, { layThongBaoLoi } from '../services/api';
import useDuLieu from '../hooks/useDuLieu';
import { ngayHomNay, tienViet } from '../utils/hienThi';

export default function BaoCaoPage() {
  const [from, setFrom] = useState(() => ngayHomNay().slice(0, 8) + '01');
  const [to, setTo] = useState(ngayHomNay);
  const { data, error, loading } = useDuLieu('/quan-tri/bao-cao', { tuNgay: from, denNgay: to });
  const [exportError, setExportError] = useState('');
  const [exporting, setExporting] = useState(false);

  async function exportCsv() {
    setExporting(true);
    setExportError('');
    try {
      const result = await api.get('/quan-tri/bao-cao.csv', { params: { tuNgay: from, denNgay: to }, responseType: 'blob' });
      const url = URL.createObjectURL(result.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'thu-hoan-' + from + '_' + to + '.csv';
      link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (err) {
      setExportError(layThongBaoLoi(err));
    } finally {
      setExporting(false);
    }
  }

  const max = Math.max(1, ...(data?.theoNgay || []).flatMap(d => [Number(d.thu), Number(d.hoan)]));

  return (
    <div className="space-y-6 text-slate-800 font-sans antialiased">
      {/* HEADER VÀ BỘ LỌC KHOẢNG THỜI GIAN */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-6 rounded-2xl bg-white border border-slate-200 shadow-sm">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 text-xl font-bold">
            📈
          </span>
          <div>
            <h1 className="text-xl font-bold tracking-tight text-slate-900">
              Báo Cáo Thu / Hoàn Tiền
            </h1>
            <p className="text-xs text-slate-500">
              Thống kê chi tiết dòng tiền thực thu, tiền hoàn trả và thu ròng theo khoảng thời gian
            </p>
          </div>
        </div>

        {/* Form lọc thời gian & Nút xuất CSV */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-xs">
            <span className="text-slate-500 font-medium">Từ:</span>
            <input
              type="date"
              required
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              className="bg-transparent text-slate-900 font-semibold focus:outline-none cursor-pointer"
            />
            <span className="text-slate-400 font-medium ml-1">Đến:</span>
            <input
              type="date"
              required
              value={to}
              onChange={(e) => setTo(e.target.value)}
              className="bg-transparent text-slate-900 font-semibold focus:outline-none cursor-pointer"
            />
          </div>

          <button
            disabled={exporting || loading || !!error}
            onClick={exportCsv}
            className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-semibold text-xs shadow-sm transition active:scale-95"
          >
            <span>📥</span> {exporting ? 'Đang xuất...' : 'Xuất file CSV'}
          </button>
        </div>
      </div>

      {/* THÔNG BÁO LỖI & LOADING */}
      {(error || exportError) && (
        <div role="alert" className="flex items-center gap-3 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs font-medium">
          <span className="text-base">⚠️</span>
          <p className="flex-1">{error || exportError}</p>
        </div>
      )}

      {loading && (
        <div className="flex items-center justify-center p-12 rounded-2xl bg-white border border-slate-200 text-slate-500 text-sm shadow-sm">
          <div className="flex items-center gap-3">
            <span className="h-4 w-4 rounded-full bg-indigo-600 animate-ping" />
            <span>Đang tổng hợp dữ liệu báo cáo tài chính...</span>
          </div>
        </div>
      )}

      {/* NỘI DUNG BÁO CÁO */}
      {data && !error && (
        <>
          {/* 3 THẺ THỐNG KÊ TỔNG QUAN */}
          <div className="grid gap-4 sm:grid-cols-3">
            <div className="rounded-2xl bg-white border border-slate-200 p-5 shadow-sm space-y-2">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Tiền đã thu</span>
              <p className="text-2xl font-extrabold text-emerald-600">{tienViet(data.tongThu)}</p>
              <p className="text-[11px] text-slate-400">Tổng doanh thu nhận được trong kỳ</p>
            </div>

            <div className="rounded-2xl bg-white border border-slate-200 p-5 shadow-sm space-y-2">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Tiền đã hoàn</span>
              <p className="text-2xl font-extrabold text-rose-600">{tienViet(data.tongHoan)}</p>
              <p className="text-[11px] text-slate-400">Tổng tiền đã hoàn trả cho đơn hủy</p>
            </div>

            <div className="rounded-2xl bg-white border border-slate-200 p-5 shadow-sm space-y-2">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Thu ròng thực tế</span>
              <p className="text-2xl font-extrabold text-indigo-600">{tienViet(data.thuRong)}</p>
              <p className="text-[11px] text-slate-400">Tiền đã thu trừ tổng tiền hoàn</p>
            </div>
          </div>

          {/* BiỂU ĐỒ NẰM NGANG BIỂU DIỄN THEO NGÀY */}
          <section className="rounded-2xl bg-white border border-slate-200 p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h2 className="text-base font-bold text-slate-900">Biểu đồ giao dịch theo ngày</h2>
              <div className="flex items-center gap-4 text-xs font-medium">
                <span className="flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-indigo-600" /> Tiền thu</span>
                <span className="flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-amber-500" /> Tiền hoàn</span>
              </div>
            </div>

            <div className="max-h-[380px] space-y-3.5 overflow-y-auto pr-2">
              {data.theoNgay.map((day) => (
                <div key={day.ngay} className="grid grid-cols-[100px_1fr] items-center gap-4 text-xs">
                  <span className="font-semibold text-slate-600">{day.ngay}</span>
                  <div className="space-y-1 bg-slate-50 p-2 rounded-xl border border-slate-100">
                    <div
                      className="h-2.5 rounded-full bg-indigo-600 transition-all duration-300"
                      title={'Thu: ' + tienViet(day.thu)}
                      style={{ width: `${Math.max(2, (Number(day.thu) / max) * 100)}%` }}
                    />
                    <div
                      className="h-2.5 rounded-full bg-amber-500 transition-all duration-300"
                      title={'Hoàn: ' + tienViet(day.hoan)}
                      style={{ width: `${Math.max(2, (Number(day.hoan) / max) * 100)}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </section>

          {/* BẢNG CHI TIẾT SỐ LIỆU */}
          <div className="rounded-2xl bg-white border border-slate-200 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100 font-bold text-slate-900 text-sm">
              Chi tiết bảng số liệu theo từng ngày
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-700">
                <thead className="bg-slate-50 text-slate-500 font-bold uppercase tracking-wider border-b border-slate-200">
                  <tr>
                    <th className="px-6 py-3.5">Ngày</th>
                    <th className="px-6 py-3.5 text-emerald-600">Tiền thu</th>
                    <th className="px-6 py-3.5 text-rose-600">Tiền hoàn</th>
                    <th className="px-6 py-3.5 text-indigo-600">Thu ròng</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 font-medium">
                  {data.theoNgay.map((day) => (
                    <tr key={day.ngay} className="hover:bg-slate-50/80 transition">
                      <td className="px-6 py-3.5 font-bold text-slate-900">{day.ngay}</td>
                      <td className="px-6 py-3.5 text-emerald-600 font-semibold">{tienViet(day.thu)}</td>
                      <td className="px-6 py-3.5 text-rose-600 font-semibold">{tienViet(day.hoan)}</td>
                      <td className="px-6 py-3.5 text-indigo-600 font-bold">{tienViet(day.rong)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* GHI CHÚ BÁO CÁO */}
          <div className="rounded-xl bg-slate-100 p-4 border border-slate-200 text-xs text-slate-600 leading-relaxed">
            💡 <strong>Ghi chú tài chính:</strong> Tiền thu được ghi nhận vào đúng mốc thời gian khách thanh toán. Khoản tiền chờ hoàn hiện tại của toàn hệ thống: <strong className="text-amber-700">{tienViet(data.choHoanHienTai)}</strong>. Báo cáo này phản ánh dòng tiền vào/ra (Cash flow) và chưa trừ các chi phí vận hành khác.
          </div>
        </>
      )}
    </div>
  );
}