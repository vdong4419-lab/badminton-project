import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useParams } from 'react-router-dom';
import { PhienDangNhapProvider } from './context/PhienDangNhap';
import { usePhienDangNhap } from './context/PhienContext';
import QuanTriDon from './pages/QuanTriDon';
import TongQuanPage from './pages/TongQuanPage';
import DonDatPage from './pages/DonDatPage';
import LichSanPage from './pages/LichSanPage';
import SanPage from './pages/SanPage';
import BaoCaoPage from './pages/BaoCaoPage';
import ThuCaPage from './pages/ThuCaPage';
import TaiKhoanPage from './pages/TaiKhoanPage';
import NhatKyPage from './pages/NhatKyPage';
import TrangChu from './pages/TrangChu';
import DangNhap from './pages/DangNhap';
import DangKy from './pages/DangKy';
import LichSuDatSan from './pages/LichSuDatSan';
import TrangDatLich from './pages/TrangDatLich';
import CaNhanPage from './pages/CaNhanPage';
import ThanhToanPage from './pages/ThanhToanPage';
import OtpPage from './pages/OtpPage';
import ThongBaoPage from './pages/ThongBaoPage';
import DanhGiaPage from './pages/DanhGiaPage';
import ChiTietSanPage from './pages/ChiTietSanPage';

function CanDangNhap({ children, admin = false, vanHanh = false }) {
  const { nguoiDung, dangTai, loiPhien } = usePhienDangNhap();
  if (dangTai) return <p className="p-8">Đang kiểm tra phiên đăng nhập...</p>;
  if (loiPhien) return <p role="alert" className="p-8 text-red-700">{loiPhien}</p>;
  if (!nguoiDung) return <Navigate to="/dang-nhap" replace />;
  if (vanHanh && !['ADMIN','STAFF'].includes(nguoiDung.vaiTro)) return <Navigate to="/" replace />;
  if (admin && nguoiDung.vaiTro !== 'ADMIN') return <div className="p-8"><h1 className="text-xl font-bold">Bạn không có quyền quản trị</h1><a href="/" className="underline">Về trang chủ</a></div>;
  return children;
}

function TrangQuanTriMacDinh() {
  const { nguoiDung } = usePhienDangNhap();
  return <Navigate to={nguoiDung?.vaiTro === 'ADMIN' ? 'tong-quan' : 'lich-san'} replace />;
}

function ThanhToanTheoDon() {
  const { donId } = useParams();
  return <ThanhToanPage key={donId} />;
}
function DanhGiaTheoDon() {
  const { donId } = useParams();
  return <DanhGiaPage key={donId} />;
}

function App() {
  return (
    <PhienDangNhapProvider><Router>
      <Routes>
        <Route path="/" element={<TrangChu />} />
        <Route path="/dang-nhap" element={<DangNhap />} />
        <Route path="/dang-ky" element={<DangKy />} />
        <Route path="/xac-minh-email" element={<OtpPage key="verify" />} />
        <Route path="/quen-mat-khau" element={<OtpPage key="reset" datLai />} />
        <Route path="/san/:sanId" element={<ChiTietSanPage />} />
        <Route path="/thong-bao" element={<CanDangNhap><ThongBaoPage /></CanDangNhap>} />
        <Route path="/danh-gia/:donId" element={<CanDangNhap><DanhGiaTheoDon /></CanDangNhap>} />
        <Route path="/lich-su" element={<CanDangNhap><LichSuDatSan /></CanDangNhap>} />
        <Route path="/ca-nhan" element={<CanDangNhap><CaNhanPage /></CanDangNhap>} />
        <Route path="/thanh-toan/:donId" element={<CanDangNhap><ThanhToanTheoDon /></CanDangNhap>} />
        <Route path="/dat-lich/:sanId" element={<CanDangNhap><TrangDatLich /></CanDangNhap>} />
        <Route path="/quan-tri" element={<CanDangNhap vanHanh><QuanTriDon /></CanDangNhap>}>
          <Route index element={<TrangQuanTriMacDinh />} />
          <Route path="tong-quan" element={<CanDangNhap admin><TongQuanPage /></CanDangNhap>} />
          <Route path="don-dat" element={<DonDatPage />} />
          <Route path="lich-san" element={<LichSanPage />} />
          <Route path="thu-ca" element={<ThuCaPage />} />
          <Route path="san" element={<CanDangNhap admin><SanPage /></CanDangNhap>} />
          <Route path="bao-cao" element={<CanDangNhap admin><BaoCaoPage /></CanDangNhap>} />
          <Route path="tai-khoan" element={<CanDangNhap admin><TaiKhoanPage /></CanDangNhap>} />
          <Route path="nhat-ky" element={<CanDangNhap admin><NhatKyPage /></CanDangNhap>} />
        </Route>
      </Routes>
    </Router></PhienDangNhapProvider>
  );
}

export default App;
