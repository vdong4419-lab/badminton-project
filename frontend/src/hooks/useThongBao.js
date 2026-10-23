import { useEffect, useState } from 'react';
import api, { layThongBaoLoi } from '../services/api';
import { usePhienDangNhap } from '../context/PhienContext';

export default function useThongBao(page = 0) {
  const { nguoiDung } = usePhienDangNhap();
  const email = nguoiDung?.email;
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [reload, setReload] = useState(0);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    const changed = () => setReload(v => v + 1);
    window.addEventListener('thong-bao-thay-doi', changed);
    return () => window.removeEventListener('thong-bao-thay-doi', changed);
  }, []);
  useEffect(() => {
    if (!email) return undefined;
    const controller = new AbortController(); let running = false;
    async function load() {
      if (running) return;
      running = true;
      try {
        const { data } = await api.get('/thong-bao', { params: { page }, signal: controller.signal });
        if (!controller.signal.aborted) { setResult({ email, page, data }); setError(''); }
      } catch (err) { if (!controller.signal.aborted) setError(layThongBaoLoi(err, 'Chưa tải được thông báo.')); }
      finally { running = false; }
    }
    void load();
    const timer = setInterval(load, 30000);
    return () => { controller.abort(); clearInterval(timer); };
  }, [email, page, reload]);
  async function danhDau(id) {
    if (busy) return false;
    setBusy(true); setError('');
    try { await api.put(id ? `/thong-bao/${id}/da-doc` : '/thong-bao/doc-tat-ca'); window.dispatchEvent(new Event('thong-bao-thay-doi')); return true; }
    catch (err) { setError(layThongBaoLoi(err)); return false; }
    finally { setBusy(false); }
  }
  return { data: result?.email === email && result?.page === page ? result.data : null, error, busy, danhDau, taiLai: () => setReload(v => v + 1), email };
}
