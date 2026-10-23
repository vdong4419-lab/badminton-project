package com.dong.dat_san.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class NhatKyQuanTri {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public NhatKyQuanTri(JdbcTemplate jdbc, Clock clock) { this.jdbc=jdbc; this.clock=clock; }
    public void ghi(String action, String target, String note) {
        String email=SecurityContextHolder.getContext().getAuthentication().getName();
        jdbc.update("""
                INSERT INTO nhat_ky_quan_tri(nguoi_thuc_hien_id,hanh_dong,doi_tuong,noi_dung,ngay_tao)
                SELECT id,?,?,?,? FROM nguoi_dung WHERE email=?
                """,action,target,note,LocalDateTime.now(clock),email);
    }
}
