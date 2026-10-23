import { useEffect, useState } from 'react';
import api, { layThongBaoLoi } from '../services/api';
export default function useDuLieu(url, params = {}) {
  const key = JSON.stringify(params);
  const [data, setData] = useState(null);
  const [loadedKey, setLoadedKey] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      setLoading(true); setError('');
      try {
        const response = await api.get(url, { params: JSON.parse(key), signal: controller.signal });
        if (!controller.signal.aborted) { setData(response.data); setLoadedKey(url+'|'+key); }
      } catch (err) { if (!controller.signal.aborted) setError(layThongBaoLoi(err)); }
      finally { if (!controller.signal.aborted) setLoading(false); }
    }
    void load();
    return () => controller.abort();
  }, [url, key, version]);
  return { data: loadedKey === url+'|'+key ? data : null, error, loading, reload: () => setVersion(value => value + 1) };
}
