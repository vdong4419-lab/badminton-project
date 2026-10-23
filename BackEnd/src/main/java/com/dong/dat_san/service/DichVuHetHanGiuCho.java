package com.dong.dat_san.service;

import com.dong.dat_san.repository.*;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DichVuHetHanGiuCho {
    private final KhoDuLieuDatSan orders;
    private final KhoDuLieuChiTietDatSan details;
    private final KhoDuLieuThanhToan payments;
    private final KhoDuLieuGiuCho holds;
    private final KhoDuLieuLichSan slots;
    private final ChinhSachDatSan policy;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public DichVuHetHanGiuCho(KhoDuLieuDatSan orders, KhoDuLieuChiTietDatSan details,
            KhoDuLieuThanhToan payments, KhoDuLieuGiuCho holds, KhoDuLieuLichSan slots,
            ChinhSachDatSan policy, JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.orders = orders; this.details = details; this.payments = payments;
        this.holds = holds; this.slots = slots; this.policy = policy; this.jdbc = jdbc;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void quet() {
        List<Long> ids = jdbc.queryForList("""
                SELECT d.id FROM dat_san d
                WHERE d.trang_thai = 'CHO_XAC_NHAN'
                  AND NOT EXISTS (SELECT 1 FROM thanh_toan t WHERE t.dat_san_id=d.id AND t.trang_thai='DA_THANH_TOAN')
                  AND (d.ngay_tao <= ? OR EXISTS (
                    SELECT 1 FROM chi_tiet_dat_san c JOIN lich_san l ON l.id=c.lich_san_id
                    JOIN khung_gio k ON k.id=l.khung_gio_id
                    WHERE c.dat_san_id=d.id AND TIMESTAMP(l.ngay_choi,k.gio_bat_dau) <= ?))
                ORDER BY d.id LIMIT 200
                """, Long.class, policy.now().minusMinutes(policy.getHoldMinutes()), policy.now());
        ids.forEach(this::xuLy);
    }

    public void xuLy(Long id) {
        transaction.executeWithoutResult(status -> {
            var booking = orders.findLockedById(id).orElse(null);
            if (booking == null) return;
            var items = details.findByDatSanIdOrderByIdAsc(id);
            var payment = payments.findByDatSanId(id).orElse(null);
            if (!policy.hetHan(booking, items, payment)) return;
            if (!items.isEmpty()) {
                slots.khoaLich(items.stream().map(item -> item.getLichSan().getId()).sorted().toList());
            }
            booking.setTrangThai("DA_HUY");
            booking.setLyDoHuy("Hết hạn giữ suất chờ xác nhận");
            booking.setNgayHuy(policy.now());
            holds.giaiPhongDon(id);
            orders.ghiLichSu(id, null, "CHO_XAC_NHAN", "DA_HUY",
                    "Hệ thống tự hủy: hết hạn giữ suất chờ xác nhận", policy.now());
        });
    }
}
