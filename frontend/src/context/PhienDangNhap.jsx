import { useEffect, useState } from 'react';
import PhienContext from './PhienContext';
import api from '../services/api';
import { phienConHan, xoaPhien } from '../utils/hienThi';
export function PhienDangNhapProvider({ children }) {
  const [nguoiDung, setNguoiDung] = useState(null);
  const [dangTai, setDangTai] = useState(phienConHan);
  const [loiPhien, setLoiPhien] = useState('');
  useEffect(() => {
    const controller = new AbortController();
    const token = localStorage.getItem('token');
    const daXoa = () => { setNguoiDung(null); setDangTai(false); };
    window.addEventListener('phien-da-xoa', daXoa);
    async function taiPhien() {
      if (!phienConHan()) return;
      try {
        const response = await api.get('/xac-thuc/toi', { signal: controller.signal });
        if (!controller.signal.aborted && token === localStorage.getItem('token')) setNguoiDung(response.data);
      } catch (error) {
        if (!controller.signal.aborted) {
          if (error.response?.status === 401) xoaPhien();
          else setLoiPhien('Chưa kiểm tra được phiên đăng nhập. Vui lòng tải lại trang.');
        }
      } finally { if (!controller.signal.aborted) setDangTai(false); }
    }
    void taiPhien();
    return () => { controller.abort(); window.removeEventListener('phien-da-xoa', daXoa); };
  }, []);
  function dangNhap(data) {
    localStorage.setItem('token', data.token);
    localStorage.setItem('userEmail', data.email);
    localStorage.setItem('hoTen', data.hoTen || '');
    setNguoiDung({ email: data.email, hoTen: data.hoTen, vaiTro: data.vaiTro });
    setLoiPhien(''); setDangTai(false);
  }
  function dangXuat() { xoaPhien(); setNguoiDung(null); }
  function capNhatHoSo(data) {
    localStorage.setItem('hoTen', data.hoTen || '');
    setNguoiDung(current => current ? { ...current, hoTen: data.hoTen } : current);
  }
  return <PhienContext.Provider value={{ nguoiDung, dangTai, loiPhien, dangNhap, dangXuat, capNhatHoSo }}>{children}</PhienContext.Provider>;
}
