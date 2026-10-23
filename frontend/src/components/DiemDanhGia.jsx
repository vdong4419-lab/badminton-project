export default function DiemDanhGia({ diem = 0, soLuong = 0 }) {
  const score = Math.min(5, Math.max(0, Number(diem) || 0));
  const count = Math.max(0, Number(soLuong) || 0);
  return <span className="inline-flex flex-wrap items-center gap-2 text-xs">
    <span aria-hidden="true" className="inline-flex text-base leading-none tracking-wider">{[0,1,2,3,4].map(index => <span key={index} className="relative inline-block text-slate-200">★<span className="absolute inset-y-0 left-0 overflow-hidden text-amber-500" style={{ width: `${count ? Math.max(0,Math.min(1,score-index))*100 : 0}%` }}>★</span></span>)}</span>
    {count ? <><strong className="text-slate-900">{score.toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}/5</strong><span className="text-slate-500">({count.toLocaleString('vi-VN')} đánh giá)</span></> : <span className="text-slate-500">Chưa có đánh giá</span>}
  </span>;
}
