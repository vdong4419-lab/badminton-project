package com.dong.dat_san;

import com.dong.dat_san.entity.*;
import com.dong.dat_san.service.ChinhSachDatSan;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ChinhSachDatSanTests {
    private ChinhSachDatSan policy(String instant) {
        return new ChinhSachDatSan(Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Ho_Chi_Minh")), 15);
    }
    private ThucTheDatSan booking() {
        var booking = new ThucTheDatSan();
        booking.setNgayTao(LocalDateTime.parse("2026-10-17T11:45:00"));
        return booking;
    }
    private List<ChiTietDatSan> items(ThucTheDatSan booking, String time) {
        var frame = new ThucTheKhungGio();
        frame.setGioBatDau(LocalTime.parse(time));
        frame.setGioKetThuc(LocalTime.parse(time).plusMinutes(30));
        var slot = new ThucTheLichSan();
        slot.setNgayChoi(LocalDate.parse("2026-10-17"));
        slot.setKhungGio(frame);
        slot.setGia(new BigDecimal("50000"));
        return List.of(new ChiTietDatSan(booking, slot));
    }
    @Test
    void expiresAtFifteenMinutesButNotOneSecondEarlier() {
        var booking = booking();
        var items = items(booking, "13:00");
        assertThat(policy("2026-10-17T04:59:59Z").hetHan(booking, items, null)).isFalse();
        assertThat(policy("2026-10-17T05:00:00Z").hetHan(booking, items, null)).isTrue();
    }
    @Test
    void holdCannotExtendPastFirstSlotStart() {
        var booking = booking();
        var policy = policy("2026-10-17T04:50:00Z");
        var items = items(booking, "11:50");
        assertThat(policy.hetHanLuc(booking, items, null)).isEqualTo(LocalDateTime.parse("2026-10-17T11:50:00"));
        assertThat(policy.hetHan(booking, items, null)).isTrue();
    }
    @Test
    void confirmedAndPaidOrdersHaveNoHoldDeadline() {
        var booking = booking();
        var items = items(booking, "13:00");
        var policy = policy("2026-10-17T05:30:00Z");
        booking.setTrangThai("DA_XAC_NHAN");
        assertThat(policy.hetHanLuc(booking, items, null)).isNull();
        booking.setTrangThai("CHO_XAC_NHAN");
        var payment = new ThanhToan();
        payment.setTrangThai("DA_THANH_TOAN");
        assertThat(policy.hetHan(booking, items, payment)).isFalse();
        assertThatIllegalArgumentException().isThrownBy(() -> new ChinhSachDatSan(Clock.systemUTC(), 0));
    }
}
