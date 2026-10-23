package com.dong.dat_san.service;

import com.dong.dat_san.entity.*;
import java.time.*;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChinhSachDatSan {
    private final Clock clock;
    private final long holdMinutes;
    public ChinhSachDatSan(Clock clock, @Value("${app.booking.hold-minutes:15}") long holdMinutes) {
        if (holdMinutes < 1 || holdMinutes > 120) throw new IllegalArgumentException("Thời hạn giữ suất phải từ 1 đến 120 phút.");
        this.clock = clock;
        this.holdMinutes = holdMinutes;
    }
    public LocalDateTime now() { return LocalDateTime.now(clock); }
    public long getHoldMinutes() { return holdMinutes; }
    public static boolean daThu(ThanhToan payment) {
        return payment != null && "DA_THANH_TOAN".equals(payment.getTrangThai());
    }
    public LocalDateTime hetHanLuc(ThucTheDatSan don, List<ChiTietDatSan> details, ThanhToan payment) {
        if (!"CHO_XAC_NHAN".equals(don.getTrangThai()) || daThu(payment)) return null;
        LocalDateTime deadline = don.getNgayTao().plusMinutes(holdMinutes);
        for (var detail : details) {
            LocalDateTime start = batDau(detail.getLichSan());
            if (start.isBefore(deadline)) deadline = start;
        }
        return deadline;
    }
    public boolean hetHan(ThucTheDatSan don, List<ChiTietDatSan> details, ThanhToan payment) {
        var deadline = hetHanLuc(don, details, payment);
        return deadline != null && !deadline.isAfter(now());
    }
    public boolean chuaBatDau(List<ChiTietDatSan> details) {
        var now = now();
        return !details.isEmpty() && details.stream().allMatch(d -> batDau(d.getLichSan()).isAfter(now));
    }
    public boolean coTheHuy(ThucTheDatSan don, List<ChiTietDatSan> details, ThanhToan payment) {
        return Set.of("CHO_XAC_NHAN", "DA_XAC_NHAN").contains(don.getTrangThai())
                && !hetHan(don, details, payment) && chuaBatDau(details);
    }
    public boolean coTheHoanThanh(ThucTheDatSan don, List<ChiTietDatSan> details, ThanhToan payment) {
        var now = now();
        return "DA_XAC_NHAN".equals(don.getTrangThai()) && daThu(payment) && !details.isEmpty()
                && details.stream().allMatch(d -> !d.getLichSan().getNgayChoi()
                        .atTime(d.getLichSan().getKhungGio().getGioKetThuc()).isAfter(now));
    }
    public static LocalDateTime batDau(ThucTheLichSan slot) {
        return slot.getNgayChoi().atTime(slot.getKhungGio().getGioBatDau());
    }
}
