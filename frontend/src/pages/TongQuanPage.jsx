import { useState } from 'react';
import { Link } from 'react-router-dom';
import useDuLieu from '../hooks/useDuLieu';
import { ngayHomNay, tienViet } from '../utils/hienThi';

export default function TongQuanPage() {
  const [day, setDay] = useState(ngayHomNay);
  const { data, error, loading, reload } = useDuLieu('/quan-tri/tong-quan', { ngay: day });

  const total = Number(data?.suat?.tongSuat || 0);
  const occupied = Number(data?.suat?.daDat || 0);
  const occupancyPercent = total ? Math.round((occupied / total) * 100) : 0;

  return (
    <div className="space-y-6 text-slate-800 font-sans antialiased">
      {/* HEADER & BỘ LỌC NGÀY - TONE SÁNG CỦA DASHBOARD */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-6 rounded-2xl bg-white border border-slate-200 shadow-sm">
        <div className="space-y-1">
          <div className="flex items-center gap-2.5">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 text-xl font-bold">
              📊
            </span>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-slate-900">
                Tổng Quan Vận Hành
              </h1>
              <p className="text-xs text-slate-500">
                Theo dõi tình hình kinh doanh, công suất sân và các công việc cần xử lý trong ngày
              </p>
            </div>
          </div>
        </div>

        {/* Thanh chọn ngày & Nút làm mới */}
        <div className="flex flex-wrap items-center gap-2.5">
          <div className="flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-xs">
            <span className="text-slate-500 font-medium">📅 Ngày theo dõi:</span>
            <input
              type="date"
              value={day}
              onChange={(e) => setDay(e.target.value)}
              required
              className="bg-transparent text-slate-900 font-semibold focus:outline-none cursor-pointer"
            />
          </div>
          {day !== ngayHomNay && (
            <button
              onClick={() => setDay(ngayHomNay)}
              className="px-3 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-xs text-slate-700 font-semibold transition"
            >
              Hôm nay
            </button>
          )}
          <button
            onClick={reload}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs shadow-sm transition active:scale-95"
          >
            <span>🔄</span> Tải lại
          </button>
        </div>
      </div>

      {/* THÔNG BÁO LỖI VÀ LOADING */}
      {error && (
        <div role="alert" className="flex items-center gap-3 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs font-medium">
          <span className="text-base">⚠️</span>
          <p className="flex-1">{error}</p>
        </div>
      )}

      {loading && (
        <div className="flex items-center justify-center p-12 rounded-2xl bg-white border border-slate-200 text-slate-500 text-sm shadow-sm">
          <div className="flex items-center gap-3">
            <span className="h-4 w-4 rounded-full bg-indigo-600 animate-ping" />
            <span>Đang cập nhật dữ liệu vận hành...</span>
          </div>
        </div>
      )}

      {/* DỮ LIỆU BẢNG ĐIỀU HÀNH */}
      {data && (
        <>
          {/* 4 THẺ THỐNG KÊ (KPI CARDS) TONE SÁNG */}
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {/* Thẻ 1: Tổng đơn */}
            <div className="rounded-2xl bg-white border border-slate-200 p-5 shadow-sm space-y-3 hover:border-slate-300 transition">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Đơn có lịch chơi</span>
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-50 text-blue-600 text-base">🎾</span>
              </div>
              <p className="text-3xl font-extrabold text-slate-900">{data.don.tongDon}</p>
              <p className="text-[11px] text-slate-400">Tổng số đơn ghi nhận trong ngày</p>
            </div>

            {/* Thẻ 2: Chờ xác nhận */}
            <div className="rounded-2xl bg-white border border-slate-200 p-5 shadow-sm space-y-3 hover:border-slate-300 transition">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Chờ xác nhận</span>
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-50 text-amber-600 text-base">⏳</span>
              </div>
              <p className="text-3xl font-extrabold text-amber-600">{data.don.choXacNhan}</p>
              <p className="text-[11px] text-slate-400">Đơn đang chờ admin duyệt giữ lịch</p>
            </div>

            {/* Thẻ 3: Đã thu trong ngày */}
            <div className="rounded-2xl bg-white border border-slate-200 p-5 shadow-sm space-y-3 hover:border-slate-300 transition">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Đã thu trong ngày</span>
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 text-base">💵</span>
              </div>
              <p className="text-2xl font-extrabold text-emerald-600">{tienViet(data.tien.tongThu)}</p>
              <p className="text-[11px] text-slate-400">Tiền thực thu từ khách hàng</p>
            </div>

            {/* Thẻ 4: Đã hoàn trong ngày */}
            <div className="rounded-2xl bg-white border border-slate-200 p-5 shadow-sm space-y-3 hover:border-slate-300 transition">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Đã hoàn trong ngày</span>
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-50 text-rose-600 text-base">💸</span>
              </div>
              <p className="text-2xl font-extrabold text-rose-600">{tienViet(data.tien.tongHoan)}</p>
              <p className="text-[11px] text-slate-400">Tiền đã hoàn trả do hủy đơn</p>
            </div>
          </div>

          {/* KHU VỰC CHI TIẾT 2 CỘT */}
          <div className="grid gap-6 md:grid-cols-2">
            {/* Cột Trái: Mức sử dụng sân */}
            <section className="rounded-2xl bg-white border border-slate-200 p-6 space-y-5 shadow-sm flex flex-col justify-between">
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                    <span>🏟️</span> Mức sử dụng sân
                  </h2>
                  <span className="text-xs font-semibold px-2.5 py-1 rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-100">
                    {data.sanHoatDong} sân mở cửa
                  </span>
                </div>

                <div className="flex items-baseline justify-between pt-1">
                  <p className="text-4xl font-extrabold text-indigo-600">
                    {occupancyPercent}%
                  </p>
                  <p className="text-xs font-medium text-slate-600">
                    Đã đặt <strong className="text-slate-900">{occupied}</strong> / {total} suất mở
                  </p>
                </div>

                {/* Thanh phần trăm công suất */}
                <div className="h-3 overflow-hidden rounded-full bg-slate-100 p-0.5 border border-slate-200">
                  <div
                    className="h-full rounded-full bg-indigo-600 transition-all duration-500"
                    style={{ width: `${occupancyPercent}%` }}
                  />
                </div>
              </div>

              <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
                <p className="text-xs text-slate-500">Xem sơ đồ lịch trống từng sân</p>
                <Link
                  to={'/quan-tri/lich-san?ngay=' + day}
                  className="inline-flex items-center gap-1 text-xs font-bold text-indigo-600 hover:text-indigo-800 transition"
                >
                  Mở lịch sân ➔
                </Link>
              </div>
            </section>

            {/* Cột Phải: Công việc cần xử lý */}
            <section className="rounded-2xl bg-white border border-slate-200 p-6 space-y-5 shadow-sm flex flex-col justify-between">
              <div className="space-y-4">
                <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <span>⚡</span> Công việc cần xử lý
                </h2>

                <div className="space-y-2.5 text-xs">
                  <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-50 border border-slate-100">
                    <span className="text-slate-600 font-medium">Đơn chờ xác nhận trong ngày:</span>
                    <strong className="text-amber-700 text-sm font-bold">{data.don.choXacNhan} đơn</strong>
                  </div>

                  <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-50 border border-slate-100">
                    <span className="text-slate-600 font-medium">Tiền chờ hoàn hiện tại:</span>
                    <strong className="text-rose-600 text-sm font-bold">{tienViet(data.tien.choHoanHienTai)}</strong>
                  </div>

                  <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-50 border border-slate-100">
                    <span className="text-slate-600 font-medium">Thu ròng trong ngày:</span>
                    <strong className="text-emerald-700 text-sm font-bold">{tienViet(data.tien.thuRong)}</strong>
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
                <p className="text-xs text-slate-500">Duyệt đơn và hoàn tiền cho khách</p>
                <Link
                  to="/quan-tri/don-dat"
                  className="inline-flex items-center gap-1 text-xs font-bold text-indigo-600 hover:text-indigo-800 transition"
                >
                  Xử lý đơn & Hoàn tiền ➔
                </Link>
              </div>
            </section>
          </div>

          {/* GHI CHÚ PHÍA DƯỚI */}
          <p className="text-center text-xs text-slate-400 pt-2">
            * Số liệu tính toán dựa trên toàn bộ giao dịch trong ngày được chọn. Thu ròng = Tổng thu - Tổng hoàn.
          </p>
        </>
      )}
    </div>
  );
}