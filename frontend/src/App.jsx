import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import TrangChu from './pages/TrangChu';
import DangNhap from './pages/DangNhap';
import DangKy from './pages/DangKy';
import LichSuDatSan from './pages/LichSuDatSan';
import TrangDatLich from './pages/TrangDatLich';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<TrangChu />} />
        <Route path="/dang-nhap" element={<DangNhap />} />
        <Route path="/dang-ky" element={<DangKy />} />
        <Route path="/lich-su" element={<LichSuDatSan />} />
        <Route path="/dat-lich/:sanId" element={<TrangDatLich />} />
      </Routes>
    </Router>
  );
}

export default App;