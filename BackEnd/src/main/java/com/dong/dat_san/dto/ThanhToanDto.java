package com.dong.dat_san.dto;

import com.dong.dat_san.entity.ThanhToan;
import com.dong.dat_san.entity.ThucTheDatSan;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ThanhToanDto(String trangThai, String phuongThuc, BigDecimal soTien, String maThamChieu,
                           LocalDateTime thoiDiemThanhToan, LocalDateTime thoiDiemHoanTien, boolean choHoanTien) {
    public static ThanhToanDto from(ThucTheDatSan don, ThanhToan payment) {
        if (payment == null) {
            return new ThanhToanDto("CHUA_THANH_TOAN", null, don.getTongTien(), null, null, null, false);
        }
        return new ThanhToanDto(payment.getTrangThai(), payment.getPhuongThuc(), payment.getSoTien(),
                payment.getMaThamChieu(), payment.getThoiDiemThanhToan(), payment.getThoiDiemHoanTien(),
                "DA_HUY".equals(don.getTrangThai()) && "DA_THANH_TOAN".equals(payment.getTrangThai()));
    }
}
