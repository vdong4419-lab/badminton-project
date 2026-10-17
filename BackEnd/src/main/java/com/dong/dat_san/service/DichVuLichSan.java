package com.dong.dat_san.service;

import java.time.Clock;
import java.time.LocalDate;
import com.dong.dat_san.dto.YeuCauTaoSan;
import com.dong.dat_san.entity.San;
import com.dong.dat_san.repository.SanRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DichVuLichSan {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final SanRepository courts;

    public DichVuLichSan(JdbcTemplate jdbc, Clock clock, SanRepository courts) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.courts = courts;
    }

    @Transactional
    public San taoSan(YeuCauTaoSan request) {
        San court = new San();
        court.setTenSan(request.tenSan().trim());
        court.setGiaMoiGio(request.giaMoiGio());
        courts.saveAndFlush(court);
        moLich();
        return court;
    }

    // Chỉ bổ sung suất còn thiếu; không mở lại suất bị đóng hay đổi giá đã điều chỉnh.
    @Transactional
    public void moLich() {
        LocalDate today = LocalDate.now(clock);
        for (int offset = 0; offset < 7; offset++) {
            jdbc.update("""
                    INSERT INTO lich_san (san_id, khung_gio_id, ngay_choi, gia)
                    SELECT s.id, k.id, ?, ROUND(s.gia_moi_gio / 2, 2)
                    FROM san s CROSS JOIN khung_gio k WHERE s.dang_hoat_dong = TRUE
                    ON DUPLICATE KEY UPDATE id = lich_san.id
                    """, today.plusDays(offset));
        }
    }
}
