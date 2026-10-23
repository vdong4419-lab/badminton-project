package com.dong.dat_san.service;

import com.dong.dat_san.entity.ThucTheDatSan;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class QuyenVanHanh {
    private final Clock clock;
    private final JdbcTemplate jdbc;
    public QuyenVanHanh(Clock clock, JdbcTemplate jdbc) { this.clock=clock; this.jdbc=jdbc; }
    public boolean laNhanVien() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"))
                && auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
    public LocalDate homNay() { return LocalDate.now(clock); }
    public LocalDate ngayDuocXem(LocalDate day) {
        if (laNhanVien()) {
            if (day != null && !day.equals(homNay())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Nhân viên chỉ được vận hành lịch hôm nay.");
            return homNay();
        }
        return day;
    }
    public void kiemTraDon(ThucTheDatSan order) {
        if (laNhanVien() && jdbc.queryForObject("""
                SELECT COUNT(*) FROM chi_tiet_dat_san c JOIN lich_san l ON l.id=c.lich_san_id
                WHERE c.dat_san_id=? AND l.ngay_choi=?
                """, Long.class, order.getId(), homNay()) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Đơn không thuộc lịch làm việc hôm nay.");
        }
    }
}
