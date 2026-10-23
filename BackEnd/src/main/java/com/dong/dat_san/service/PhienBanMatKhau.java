package com.dong.dat_san.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PhienBanMatKhau {
    private final JdbcTemplate jdbc;
    public PhienBanMatKhau(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public long hienTai(String email) {
        return jdbc.queryForObject("""
                SELECT COALESCE(p.phien_ban,0) FROM nguoi_dung n
                LEFT JOIN phien_ban_mat_khau p ON p.nguoi_dung_id=n.id WHERE n.email=?
                """, Long.class,email);
    }
    public void thayDoi(Long userId) {
        jdbc.update("""
                INSERT INTO phien_ban_mat_khau(nguoi_dung_id,phien_ban) VALUES (?,1)
                ON DUPLICATE KEY UPDATE phien_ban=phien_ban+1
                """,userId);
    }
}
