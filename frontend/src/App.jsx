import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { phienConHan } from './utils/hienThi';
import TrangChu from './pages/TrangChu';
import DangNhap from './pages/DangNhap';
import DangKy from './pages/DangKy';
import LichSuDatSan from './pages/LichSuDatSan';
import TrangDatLich from './pages/TrangDatLich';

function CanDangNhap({ children }) {
  return phienConHan() ? children : <Navigate to="/dang-nhap" replace />;
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<TrangChu />} />
        <Route path="/dang-nhap" element={<DangNhap />} />
        <Route path="/dang-ky" element={<DangKy />} />
        <Route path="/lich-su" element={<CanDangNhap><LichSuDatSan /></CanDangNhap>} />
        <Route path="/dat-lich/:sanId" element={<CanDangNhap><TrangDatLich /></CanDangNhap>} />
      </Routes>
    </Router>
  );
}

export default App;
