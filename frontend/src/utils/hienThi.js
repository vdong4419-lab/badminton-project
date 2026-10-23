export function ngayHomNay() {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date());
  const get = type => parts.find(part => part.type === type).value;
  return `${get('year')}-${get('month')}-${get('day')}`;
}

export function tienViet(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value || 0));
}

export function phienConHan() {
  try {
    const token = localStorage.getItem('token');
    if (!token) return false;
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return typeof payload.exp === 'number' && payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

export function xoaPhien() {
  ['token', 'userEmail', 'hoTen', 'vaiTro'].forEach(key => localStorage.removeItem(key));
  window.dispatchEvent(new Event('phien-da-xoa'));
}

export function thoiGianViet(value) {
  if (!value) return '';
  const zoned = /Z$|[+-]\d{2}:\d{2}$/.test(value) ? value : value + '+07:00';
  return new Date(zoned).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' });
}

export function tenTrangThaiDon(value) {
  return { CHO_XAC_NHAN: 'Chờ xác nhận', DA_XAC_NHAN: 'Đã xác nhận', DA_HOAN_THANH: 'Hoàn thành', DA_HUY: 'Đã hủy' }[value] || value;
}

export function tenTrangThaiTien(payment) {
  if (payment?.choHoanTien) return 'Chờ hoàn tiền';
  return { CHUA_THANH_TOAN: 'Chưa thanh toán', DA_THANH_TOAN: 'Đã thanh toán', DA_HOAN_TIEN: 'Đã hoàn tiền' }[payment?.trangThai] || 'Chưa thanh toán';
}
