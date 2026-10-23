import { Link } from 'react-router-dom';
import { usePhienDangNhap } from '../context/PhienContext';
export default function LienKetQuanTri() {
  const { nguoiDung } = usePhienDangNhap();
  return ['ADMIN','STAFF'].includes(nguoiDung?.vaiTro)
    ? <Link to="/quan-tri" className="inline-flex rounded-lg bg-indigo-100 px-3 py-2 text-sm font-semibold text-indigo-800 hover:bg-indigo-200">Quản trị đơn</Link>
    : null;
}
